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
                        // �Z�}�t�H�̃����[�X����X���b�h�v�[���ɋ󂫂��ł���܂ł̊ԁA
                        // execute()�Ăяo���X���b�h��acquire()��ʉ߂��Ă��܂��B
                        // ���̂��߁A�҂��L���[�̃T�C�Y��1�ȏ�m�ۂ���K�v������B
                        // (�ł�concurrentExecutor��queueCapacity��private�Ȃ̂ŃA�T�[�V�����ł��Ȃ��E�E�E)
                        semaphore.release()
                    }
                }
            })
        } catch (TaskRejectedException e) {
            // ���[�J�X���b�h�̎��s�Ɏ��s�����Ƃ��̂݁A�Z�}�t�H������B
            semaphore.release()
            throw e
        }
    }
}
