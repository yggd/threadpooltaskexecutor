package org.yggd.batch

import org.springframework.core.task.TaskRejectedException
import org.springframework.core.task.support.TaskExecutorAdapter
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.util.Assert

import java.util.concurrent.Executor
import java.util.concurrent.Semaphore

class BatchTaskExecutorAdaptor extends TaskExecutorAdapter {

    final Semaphore semaphore

    BatchTaskExecutorAdaptor(Executor concurrentExecutor) {
        super(concurrentExecutor)
        Assert.notNull(concurrentExecutor)
        Assert.isInstanceOf(ThreadPoolTaskExecutor, concurrentExecutor)
        semaphore = new Semaphore(ThreadPoolTaskExecutor.cast(concurrentExecutor).maxPoolSize)
    }

    @Override
    void execute(Runnable task) {
        semaphore.acquire()
        try {
            super.execute(new Runnable() {
                @Override
                void run() {
                    try {
                        task.run()
                    } finally {
                        // セマフォのリリースからスレッドプールに空きができるまでの間、
                        // execute()呼び出しスレッドがacquire()を通過してしまう。
                        // このため、待ちキューのサイズは1以上確保する必要がある。
                        // (でもconcurrentExecutorのqueueCapacityはprivateなのでアサーションできない・・・)
                        semaphore.release()
                    }
                }
            })
        } catch (TaskRejectedException e) {
            // ワーカスレッドの実行に失敗したときのみ、セマフォを解放。
            semaphore.release()
            throw e
        }
    }
}
