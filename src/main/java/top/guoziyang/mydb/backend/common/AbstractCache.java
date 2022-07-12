package top.guoziyang.mydb.backend.common;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import top.guoziyang.mydb.common.Error;

/**
 * AbstractCache 实现了一个引用计数策略的缓存
 */
public abstract class AbstractCache<T> {
    private HashMap<Long, T> cache;                     // 实际缓存的数据
    private HashMap<Long, Integer> references;          // 元素的引用个数
    private HashMap<Long, Boolean> getting;             // 该资源正在被获取

    private int maxResource;                            // 缓存的最大缓存资源数
    private int count = 0;                              // 缓存中元素的个数
    private Lock lock;

    public AbstractCache(int maxResource) {
        this.maxResource = maxResource;
        cache = new HashMap<>();
        references = new HashMap<>();
        getting = new HashMap<>();
        lock = new ReentrantLock();
    }

    protected T get(long key) throws Exception {
        //能获取的到话直接从cache返回，获取不到先从文件系统获取，然后再返回
        while(true) {
            lock.lock();
            if(getting.containsKey(key)) {
                // 请求的资源正在被其他线程获取
                lock.unlock();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue;
                }
                continue;
            }

            if(cache.containsKey(key)) {
                // 资源在缓存中，直接返回
                T obj = cache.get(key);

                references.put(key, references.get(key) + 1);//不是第一次，所以加1
                lock.unlock();

                return obj;//返回obj
            }

            // 尝试获取该资源
            if(maxResource > 0 && count == maxResource) {
                lock.unlock();
                throw Error.CacheFullException;
            }


            count ++;//正式get前的必要操作
            //如果缓存没满的话，就在 getting 中注册一下，该线程准备从数据源获取资源了。
            getting.put(key, true);
            lock.unlock();
            break;
        }

        T obj = null;
        try {
            obj = getForCache(key);//模板方法模式，由子类实现，从文件系统获取资源到obj
        } catch(Exception e) {
            lock.lock();
            count --;//如果出异常
            getting.remove(key);
            lock.unlock();
            throw e;
        }

        lock.lock();
        //没出异常，说明正常放入缓存中了
        getting.remove(key);//移除正在获取的状态
        cache.put(key, obj);//将刚才的obj放入缓存
        references.put(key, 1);//出次获取，上层对该该资源的引用为1

        lock.unlock();
        
        return obj;//返回obj
    }

    /**
     * 强行释放一个缓存
     */
    protected void release(long key) {
        lock.lock();//加锁，原子操作
        try {
            int ref = references.get(key)-1;
            if(ref == 0) {//这次释放完成后上层模块对这个资源的引用已经是0了
                T obj = cache.get(key);//获取到这个obj
                releaseForCache(obj);//释放的入参是obj，这才是真正的清楚
                references.remove(key);
                cache.remove(key);//移除cache,引用移除，对象还在
                count --;
            } else {//层模块对这个资还有引用，减1就行
                references.put(key, ref);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 关闭缓存，写回所有资源
     */
    protected void close() {
        lock.lock();//原子操作
        try {
            Set<Long> keys = cache.keySet();
            for (long key : keys) {
                release(key);//？？是否重复
                references.remove(key);
                cache.remove(key);
            }
        } finally {
            lock.unlock();
        }
    }


    /**
     * 当资源不在缓存时的获取行为
     *
     */
    protected abstract T getForCache(long key) throws Exception;
    /**
     * 当资源被驱逐时的写回行为
     */
    protected abstract void releaseForCache(T obj);
}
