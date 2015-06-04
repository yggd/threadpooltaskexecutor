package org.yggd.batch

import java.util.concurrent.BlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class ThreadPoolExecutorWrapper extends ThreadPoolExecutor {

    volatile Semaphore semaphore

    ThreadPoolExecutorWrapper(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                              BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler)
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        semaphore.release()
    }
}
