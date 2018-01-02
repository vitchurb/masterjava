package ru.javaops.masterjava.threadpool;

import java.util.concurrent.LinkedTransferQueue;

/**
 * Created by vch on 12.11.2017.
 */
public class BlockingQueueForGrowingExecutor<E> extends LinkedTransferQueue<E> {
    @Override
    public boolean offer(E e) {
        return tryTransfer(e);
    }
}
