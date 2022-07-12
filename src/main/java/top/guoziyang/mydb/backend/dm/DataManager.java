package top.guoziyang.mydb.backend.dm;

import top.guoziyang.mydb.backend.dm.dataItem.DataItem;
import top.guoziyang.mydb.backend.dm.logger.Logger;
import top.guoziyang.mydb.backend.dm.page.PageOne;
import top.guoziyang.mydb.backend.dm.pageCache.PageCache;
import top.guoziyang.mydb.backend.tm.TransactionManager;

public interface DataManager {
    /*
    DM 直接管理数据库 DB 文件和日志文件
    DM 的主要职责有：1) 分页管理 DB 文件，并进行缓存；
    2) 管理日志文件，保证在发生错误时可以根据日志进行恢复；
    3) 抽象 DB 文件为 DataItem 供上层模块使用，并提供缓存。



    DM 的功能其实可以归纳为两点：
    上层模块和文件系统之间的一个抽象层，
    向下直接读写文件，向上提供数据的包装；另外就是日志功能。
    可以注意到，无论是向上还是向下，DM 都提供了一个缓存的功能，用内存操作来保证效率。
     */
    DataItem read(long uid) throws Exception;
    long insert(long xid, byte[] data) throws Exception;
    void close();

    public static DataManager create(String path, long mem, TransactionManager tm) {
        PageCache pc = PageCache.create(path, mem);
        Logger lg = Logger.create(path);

        DataManagerImpl dm = new DataManagerImpl(pc, lg, tm);
        dm.initPageOne();
        return dm;
    }

    public static DataManager open(String path, long mem, TransactionManager tm) {
        PageCache pc = PageCache.open(path, mem);
        Logger lg = Logger.open(path);
        DataManagerImpl dm = new DataManagerImpl(pc, lg, tm);
        if(!dm.loadCheckPageOne()) {
            Recover.recover(tm, lg, pc);
        }
        dm.fillPageIndex();
        PageOne.setVcOpen(dm.pageOne);
        dm.pc.flushPage(dm.pageOne);

        return dm;
    }
}
