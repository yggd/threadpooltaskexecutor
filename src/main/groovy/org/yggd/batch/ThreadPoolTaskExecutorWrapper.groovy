package org.yggd.batch

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class ThreadPoolTaskExecutorWrapper extends ThreadPoolTaskExecutor {

    final Object lock = new Object()
    volatile ThreadPoolExecutorWrapper wrapper

    @Override
    ThreadPoolExecutor getThreadPoolExecutor() throws IllegalStateException {
        if (wrapper == null) {
            synchronized (lock) {
                if (wrapper == null) { // doble check locking...
                    ThreadPoolExecutor superExecutor = super.getThreadPoolExecutor()
                    wrapper = new ThreadPoolExecutorWrapper(superExecutor.getCorePoolSize(),
                            superExecutor.maximumPoolSize, superExecutor.getKeepAliveTime(TimeUnit.SECONDS),
                            TimeUnit.SECONDS, superExecutor.queue, superExecutor.threadFactory,
                            superExecutor.rejectedExecutionHandler)
                }
            }
        }
        return wrapper
    }
}
