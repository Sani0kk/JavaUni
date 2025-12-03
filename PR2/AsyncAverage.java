import java.util.*;
import java.util.concurrent.*;

public class AsyncAverage {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Введіть розмір масиву (40-60): ");
        int size = scanner.nextInt();
        if (size < 40 || size > 60) {
            System.out.println("Некоректний розмір, встановлено 50");
            size = 50;
        }

        int[] array = new int[size];
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            array[i] = random.nextInt(1001); // 0..1000
        }

        System.out.println("\nЗгенерований масив (" + size + " елементів):");
        System.out.println(Arrays.toString(array) + "\n");

        int numThreads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        List<Future<PartialResult>> futures = new ArrayList<>();
        int partSize = size / numThreads;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            int start = i * partSize;
            int end = (i == numThreads - 1) ? size : start + partSize;
            Callable<PartialResult> task = new AverageTask(array, start, end);
            Future<PartialResult> future = executor.submit(task);
            futures.add(future);
        }

        executor.shutdown();

        long totalSum = 0;
        int totalCount = 0;
        CopyOnWriteArraySet<Double> uniquePartialAverages = new CopyOnWriteArraySet<>();

        for (Future<PartialResult> future : futures) {
            while (!future.isDone()) {
                System.out.println("Очікування завершення одного з потоків... (isDone = " + future.isDone() + ")");
                Thread.sleep(50);
            }

            if (future.isCancelled()) {
                System.out.println("УВАГА: Завдання було скасовано (isCancelled = true)!");
                continue;
            }

            PartialResult result = future.get(); // отримуємо результат
            double partialAvg = (double) result.sum / result.count;
            uniquePartialAverages.add(partialAvg);

            System.out.printf("Частина оброблена: сума = %d, елементів = %d, середнє = %.2f%n",
                    result.sum, result.count, partialAvg);

            totalSum += result.sum;
            totalCount += result.count;
        }

        double average = (double) totalSum / totalCount;
        long endTime = System.currentTimeMillis();

        System.out.println("\n=== РЕЗУЛЬТАТ ===");
        System.out.printf("Середнє значення всього масиву: %.3f%n", average);
        System.out.println("Унікальні часткові середні: " + uniquePartialAverages);
        System.out.printf("Час виконання програми: %d мс%n", (endTime - startTime));
    }

    static class PartialResult {
        long sum;
        int count;

        PartialResult(long sum, int count) {
            this.sum = sum;
            this.count = count;
        }
    }

    static class AverageTask implements Callable<PartialResult> {
        private final int[] array;
        private final int start;
        private final int end;

        AverageTask(int[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        public PartialResult call() throws Exception {
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += array[i];
            }
            return new PartialResult(sum, end - start);
        }
    }
}
