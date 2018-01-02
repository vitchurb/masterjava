package ru.javaops.masterjava.threadpool;

import java.util.concurrent.*;

/**
 * Created by vch on 12.11.2017.
 * реализация ThreadPoolExecutor для случая, когда есть максимальное количество потоков, которые могут выполняться одновременно
 * Потоки запускаются по мере необходимости, а когда задач для них нет - уничтожаются.
 * Помогает паспараллеливать обработку данных, не держа запущенными лишние потоки
 * Дополнительные задачи кладутся в очередь
 * https://stackoverflow.com/questions/19528304/how-to-get-the-threadpoolexecutor-to-increase-threads-to-max-before-queueing
 */
public class ThreadPoolExecutorGrowing extends ThreadPoolExecutor {
    private void setRejectedExecutionHandlerForGrowing() {
        setRejectedExecutionHandler(new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                try {
                    executor.getQueue().put(r);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

    }

    public ThreadPoolExecutorGrowing(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new BlockingQueueForGrowingExecutor<Runnable>());
        setRejectedExecutionHandlerForGrowing();
    }

    public ThreadPoolExecutorGrowing(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new BlockingQueueForGrowingExecutor<Runnable>(), threadFactory);
        setRejectedExecutionHandlerForGrowing();
    }

    private ThreadPoolExecutorGrowing(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    private ThreadPoolExecutorGrowing(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    private ThreadPoolExecutorGrowing(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    private ThreadPoolExecutorGrowing(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

}
