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
        when: '試験開始！'
            // 50回ノーウェイトでぶん回す。
            (1..50).each { index ->
                Runnable runnable = new Runnable() {
                    @Override
                    void run() {
                        log.info "start worker thread: ${Thread.currentThread().name}"
                        int waitTime = rnd.nextInt 5000 // 最長5秒待つ（ワーカスレッド）
                        sleep waitTime
                        log.info "end worker thread: ${Thread.currentThread().name}, waitTime ${waitTime}"
                    }
                }
                log.info "execute worker: ${index}"
                batchTaskExecutorAdaptor.execute runnable
            }
            while(sourceExecutor.activeCount > 0) {
                sleep 100L // スレッドプールが全てはけるまでシャットダウン待ち。行儀のいいやり方じゃない・・・
            }
            sourceExecutor.shutdown()
        then: 'プール溢れによるRejectedExecutionExceptionが発生せず、セマフォの空きが元(最大プールサイズ)にもどること。'
            noExceptionThrown()
            batchTaskExecutorAdaptor.semaphore.availablePermits() == sourceExecutor.maxPoolSize
    }

    def "not accept case"() {
        setup:
            Executor executor = Spy ThreadPoolTaskExecutor, impl: sourceExecutor
            log.info "maxPoolSize:${sourceExecutor.maxPoolSize}"
            executor.execute(_) >> { throw new RejectedExecutionException('not accept')}
            BatchTaskExecutorAdaptor target = new BatchTaskExecutorAdaptor(executor) // テスト対象にスパイを送り込む
        when:
            target.execute(new Runnable() {
                @Override
                void run() {
                    throw new IllegalStateException('ここでは実行されないスレッド')
                }
            })
        then: 'ワーカスレッドが実行されず、セマフォのサイズも元に戻ること。'
            Exception e = thrown TaskRejectedException
            e.getMessage().contains 'not accept'
            target.semaphore.availablePermits() == executor.maxPoolSize
    }
}
