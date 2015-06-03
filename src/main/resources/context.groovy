import org.yggd.batch.BatchTaskExecutorAdaptor

beans {
    xmlns ([task: 'http://www.springframework.org/schema/task'])

    task.executor (id: 'sourceExecutor', 'pool-size': 5)
    batchTaskExecutorAdaptor (BatchTaskExecutorAdaptor, ref('sourceExecutor'))
}
