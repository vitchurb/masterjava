package ru.javaops.masterjava.matrix;

import java.util.Random;
import java.util.concurrent.*;

/**
 * gkislin
 * 03.07.2016
 */
public class MatrixUtil {

    public static int[][] concurrentMultiply(int[][] matrixA, int[][] matrixB, ExecutorService executor) throws InterruptedException, ExecutionException {
        final int matrixSize = matrixA.length;
        final int[][] matrixC = new int[matrixSize][matrixSize];

        final int[][] matrixTransposedB = new int[matrixSize][matrixSize];
        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                matrixTransposedB[j][i] = matrixB[i][j];
            }
        }

        ExecutorCompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(executor);
        CountDownLatch countDownLatch = new CountDownLatch(matrixSize);
        for (int i = 0; i < matrixSize; i++) {
            final int from = i;
            completionService.submit(() -> {
                try {
                    int[] matrixARow = matrixA[from];
                    int[] matrixCRow = matrixC[from];
                    for (int j = 0; j < matrixSize; j++) {
                        int sum = 0;
                        int[] matrixTransposedBRow = matrixTransposedB[j];
                        for (int k = 0; k < matrixSize; k++) {
                            sum += matrixARow[k] * matrixTransposedBRow[k];
                        }
                        matrixCRow[j] = sum;
                    }
                    return true;
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
        Future<Boolean> future;
        //For processing Exceptions if they occurs
        while ((future = completionService.poll()) != null) {
            future.get();
        }
        return matrixC;
    }

    public static int[][] singleThreadMultiply(int[][] matrixA, int[][] matrixB) {
        final int matrixSize = matrixA.length;
        final int[][] matrixC = new int[matrixSize][matrixSize];

        final int[][] matrixTransposedB = new int[matrixSize][matrixSize];
        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                matrixTransposedB[j][i] = matrixB[i][j];
            }
        }


        for (int i = 0; i < matrixSize; i++) {
            int[] matrixARow = matrixA[i];
            int[] matrixCRow = matrixC[i];

            for (int j = 0; j < matrixSize; j++) {
                int[] matrixTransposedBRow = matrixTransposedB[j];
                int sum = 0;
                for (int k = 0; k < matrixSize; k++) {
                    sum += matrixARow[k] * matrixTransposedBRow[k];
                }
                matrixCRow[j] = sum;
            }
        }
        return matrixC;
    }

    public static int[][] create(int size) {
        int[][] matrix = new int[size][size];
        Random rn = new Random();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrix[i][j] = rn.nextInt(10);
            }
        }
        return matrix;
    }

    public static boolean compare(int[][] matrixA, int[][] matrixB) {
        final int matrixSize = matrixA.length;
        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                if (matrixA[i][j] != matrixB[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }
}
