package org.yggd.batch

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.TaskRejectedException
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

@Slf4j
@ContextConfiguration('classpath:context.groovy')
class BatchTaskExecutorAdaptorTest extends Specification {

    @Autowired
    BatchTaskExecutorAdaptor batchTaskExecutorAdaptor

    @Autowired
    ThreadPoolTaskExecutor sourceExecutor

    def "Execute"() {
        setup:
            log.info "maxPoolSize:${sourceExecutor.maxPoolSize}"
            Random rnd = new Random()
        when: '�����J�n�I'
            // 50��m�[�E�F�C�g�łԂ�񂷁B
            (1..50).each { index ->
                Runnable runnable = new Runnable() {
                    @Override
                    void run() {
                        log.info "start worker thread: ${Thread.currentThread().name}"
                        int waitTime = rnd.nextInt 5000 // �Œ�5�b�҂i���[�J�X���b�h�j
                        sleep waitTime
                        log.info "end worker thread: ${Thread.currentThread().name}, waitTime ${waitTime}"
                    }
                }
                log.info "execute worker: ${index}"
                batchTaskExecutorAdaptor.execute runnable
            }
            while(sourceExecutor.activeCount > 0) {
                sleep 100L // �X���b�h�v�[�����S�Ă͂���܂ŃV���b�g�_�E���҂��B�s�V�̂�����������Ȃ��E�E�E
            }
            sourceExecutor.shutdown()
        then: '�v�[�����ɂ��RejectedExecutionException�����������A�Z�}�t�H�̋󂫂���(�ő�v�[���T�C�Y)�ɂ��ǂ邱�ƁB'
            noExceptionThrown()
            batchTaskExecutorAdaptor.semaphore.availablePermits() == sourceExecutor.maxPoolSize
    }

    def "not accept case"() {
        setup:
            Executor executor = Spy ThreadPoolTaskExecutor, impl: sourceExecutor
            log.info "maxPoolSize:${sourceExecutor.maxPoolSize}"
            executor.execute(_) >> { throw new RejectedExecutionException('not accept')}
            BatchTaskExecutorAdaptor target = new BatchTaskExecutorAdaptor(executor) // �e�X�g�ΏۂɃX�p�C�𑗂荞��
        when:
            target.execute(new Runnable() {
                @Override
                void run() {
                    throw new IllegalStateException('�����ł͎��s����Ȃ��X���b�h')
                }
            })
        then: '���[�J�X���b�h�����s���ꂸ�A�Z�}�t�H�̃T�C�Y�����ɖ߂邱�ƁB'
            Exception e = thrown TaskRejectedException
            e.getMessage().contains 'not accept'
            target.semaphore.availablePermits() == executor.maxPoolSize
    }
}
