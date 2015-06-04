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
class ThreadPoolTaskExecutorWrapperTest extends Specification {

    @Autowired
    ThreadPoolTaskExecutorWrapper wrapper

    def "Execute"() {
        setup:
            log.info "maxPoolSize:${wrapper.maxPoolSize}"
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
                wrapper.execute runnable
            }
            while(wrapper.wrapper.activeCount > 0) {
                sleep 100L // �X���b�h�v�[�����S�Ă͂���܂ŃV���b�g�_�E���҂��B�s�V�̂�����������Ȃ��E�E�E
            }
            wrapper.shutdown()
        then: '�v�[�����ɂ��RejectedExecutionException�����������A�Z�}�t�H�̋󂫂���(�ő�v�[���T�C�Y)�ɂ��ǂ邱�ƁB'
            noExceptionThrown()
            wrapper.wrapper.semaphore.availablePermits() == wrapper.maxPoolSize
    }
}
