import org.yggd.batch.BatchTaskExecutorAdaptor
import org.yggd.batch.ThreadPoolTaskExecutorWrapper

beans {
    xmlns ([task: 'http://www.springframework.org/schema/task'])

    task.executor (id: 'sourceExecutor', 'pool-size': 5)
    batchTaskExecutorAdaptor (BatchTaskExecutorAdaptor, ref('sourceExecutor'))

    wapperExecutor (ThreadPoolTaskExecutorWrapper) {
        corePoolSize = 5
        maxPoolSize = 5
    }
}
