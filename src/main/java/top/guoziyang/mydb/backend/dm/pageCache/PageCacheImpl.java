package top.guoziyang.mydb.backend.dm.pageCache;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import top.guoziyang.mydb.backend.common.AbstractCache;
import top.guoziyang.mydb.backend.dm.page.Page;
import top.guoziyang.mydb.backend.dm.page.PageImpl;
import top.guoziyang.mydb.backend.utils.Panic;
import top.guoziyang.mydb.common.Error;

//页面缓存实现 实现接口，继承框架
public class PageCacheImpl extends AbstractCache<Page> implements PageCache {
    
    private static final int MEM_MIN_LIM = 10;
    public static final String DB_SUFFIX = ".db";

    private RandomAccessFile file;
    private FileChannel fc;
    private Lock fileLock;

    private AtomicInteger pageNumbers;

    PageCacheImpl(RandomAccessFile file, FileChannel fileChannel, int maxResource) {
        super(maxResource);
        if(maxResource < MEM_MIN_LIM) {
            Panic.panic(Error.MemTooSmallException);
        }
        long length = 0;
        try {
            length = file.length();
        } catch (IOException e) {
            Panic.panic(e);
        }
        this.file = file;
        this.fc = fileChannel;
        this.fileLock = new ReentrantLock();
        this.pageNumbers = new AtomicInteger((int)length / PAGE_SIZE);
    }

    public int newPage(byte[] initData) {
        int pgno = pageNumbers.incrementAndGet();
        Page pg = new PageImpl(pgno, initData, null);
        flush(pg);
        return pgno;
    }

    public Page getPage(int pgno) throws Exception {
        return get((long)pgno);
    }

    /**
     * 根据pageNumber从数据库文件中读取页数据，并包裹成Page
     */
    @Override
    protected Page getForCache(long key) throws Exception {
        int pgno = (int)key;
        //根据页号获取偏移量 (Pgno-1)*size,从第一页开始
        long offset = PageCacheImpl.pageOffset(pgno);

        ByteBuffer buf = ByteBuffer.allocate(PAGE_SIZE);
        fileLock.lock();
        try {
            fc.position(offset);
            fc.read(buf);
        } catch(IOException e) {
            Panic.panic(e);
        }
        fileLock.unlock();
        return new PageImpl(pgno, buf.array(), this);
    }

    @Override
    protected void releaseForCache(Page pg) {
        if(pg.isDirty()) {
            flush(pg);
            pg.setDirty(false);
        }
    }

    //将该页强行移出缓存
    public void release(Page page) {
        release((long)page.getPageNumber());
    }


    public void flushPage(Page pg) {
        flush(pg);
    }

    //刷新脏页
    private void flush(Page pg) {
        int pgno = pg.getPageNumber();//页号
        long offset = pageOffset(pgno);//页偏移量

        fileLock.lock();//
        try {
            ByteBuffer buf = ByteBuffer.wrap(pg.getData());//将页的数据——》字节数组包装到缓冲区
            fc.position(offset);//确定偏移量
            fc.write(buf);//刷新
            fc.force(false);//刷新
        } catch(IOException e) {
            Panic.panic(e);
        } finally {
            fileLock.unlock();
        }
    }

    //根据页号截取
    public void truncateByBgno(int maxPgno) {
        long size = pageOffset(maxPgno + 1);
        try {
            file.setLength(size);
        } catch (IOException e) {
            Panic.panic(e);
        }
        pageNumbers.set(maxPgno);
    }

    //关闭资源
    @Override
    public void close() {
        super.close();
        try {
            fc.close();
            file.close();
        } catch (IOException e) {
            Panic.panic(e);
        }
    }

    //page的号码
    public int getPageNumber() {
        return pageNumbers.intValue();
    }

    //page的偏移量
    private static long pageOffset(int pgno) {
        return (pgno-1) * PAGE_SIZE;
    }
    
}
