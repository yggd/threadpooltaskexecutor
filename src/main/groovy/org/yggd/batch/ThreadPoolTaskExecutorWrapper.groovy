package org.yggd.batch

import org.springframework.core.task.TaskRejectedException
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class ThreadPoolTaskExecutorWrapper extends ThreadPoolTaskExecutor {

    final Object lock = new Object()
    volatile ThreadPoolExecutorWrapper wrapper
    volatile Semaphore semaphore

    @Override
    ThreadPoolExecutor getThreadPoolExecutor() throws IllegalStateException {
        if (wrapper == null) {
            synchronized (lock) {
                if (wrapper == null) { // doble check locking...
                    ThreadPoolExecutor superExecutor = super.getThreadPoolExecutor()
                    this.wrapper = new ThreadPoolExecutorWrapper(superExecutor.getCorePoolSize(),
                            superExecutor.maximumPoolSize, superExecutor.getKeepAliveTime(TimeUnit.SECONDS),
                            TimeUnit.SECONDS, superExecutor.queue, superExecutor.threadFactory,
                            superExecutor.rejectedExecutionHandler)
                    this.wrapper.semaphore = this.semaphore
                }
            }
        }
        return wrapper
    }

    @Override
    protected ExecutorService initializeExecutor(
            ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        ExecutorService executor = super.initializeExecutor(threadFactory, rejectedExecutionHandler)
        this.semaphore = new Semaphore(super.maxPoolSize)
        return executor;
    }

    @Override
    void execute(Runnable task) {
        this.semaphore.acquire()
        try {
            super.execute(task)
        } catch (TaskRejectedException e) {
            // ワーカスレッドの実行に失敗したときのみ、セマフォを解放。
            semaphore.release()
            throw e
        }
    }
}
