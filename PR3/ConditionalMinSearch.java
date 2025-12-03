import java.util.*;
import java.util.concurrent.*;

public class ConditionalMinSearch {
    private static final int THRESHOLD = 200_000; // поріг для послідовної обробки

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        System.out.print("Введіть кількість рядків (наприклад 5000): ");
        int rows = sc.nextInt();
        System.out.print("Введіть кількість стовпців (наприклад 5000): ");
        int cols = sc.nextInt();
        System.out.print("Мінімальне значення елементів: ");
        int minVal = sc.nextInt();
        System.out.print("Максимальне значення елементів: ");
        int maxVal = sc.nextInt();

        int[][] array = generateArray(rows, cols, minVal, maxVal);
        long first = array[0][0];
        long target = 2 * first;

        System.out.printf("%nПерший елемент: %d (подвоєне значення = %d)%n", first, target);
        System.out.println("Масив згенеровано: " + rows + " × " + cols + " = " + (rows * (long)cols) + " елементів%n");

        // === Work Stealing (Fork/Join) ===
        long start = System.nanoTime();
        Long resultStealing = forkJoinSearch(array, target);
        long timeStealing = System.nanoTime() - start;

        // === Work Dealing (ExecutorService) ===
        start = System.nanoTime();
        Long resultDealing = executorSearch(array, target);
        long timeDealing = System.nanoTime() - start;

        System.out.println("=== РЕЗУЛЬТАТ ===");
        String text = (resultStealing == null)
                ? "Немає елементів, більших за подвоєне значення першого елемента"
                : "Знайдено мінімальний елемент = " + resultStealing;
        System.out.println(text);

        System.out.printf("%nWork Stealing (Fork/Join):     %.3f мс%n", timeStealing / 1_000_000.0);
        System.out.printf("Work Dealing (ExecutorService): %.3f мс%n", timeDealing / 1_000_000.0);

        if (timeStealing < timeDealing) {
            System.out.printf("→ Work Stealing швидший на %.3f мс%n", (timeDealing - timeStealing) / 1_000_000.0);
        } else {
            System.out.printf("→ Work Dealing швидший на %.3f мс%n", (timeStealing - timeDealing) / 1_000_000.0);
        }
    }

    private static int[][] generateArray(int rows, int cols, int min, int max) {
        Random r = new Random();
        int[][] arr = new int[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                arr[i][j] = r.nextInt(max - min + 1) + min;
        return arr;
    }

    // ====================== Work Stealing (Fork/Join) ======================
    static class MinTask extends RecursiveTask<Long> {
        private final int[][] array;
        private final int startRow, endRow;
        private final long target;

        MinTask(int[][] array, int startRow, int endRow, long target) {
            this.array = array;
            this.startRow = startRow;
            this.endRow = endRow;
            this.target = target;
        }

        @Override
        protected Long compute() {
            int rowsInPart = endRow - startRow;
            if (rowsInPart * array[0].length <= THRESHOLD) {
                return sequentialSearch(startRow, endRow);
            }

            int mid = startRow + rowsInPart / 2;
            MinTask left = new MinTask(array, startRow, mid, target);
            MinTask right = new MinTask(array, mid, endRow, target);

            left.fork();                    
            Long rightRes = right.compute();
            Long leftRes = left.join();

            if (leftRes == null) return rightRes;
            if (rightRes == null) return leftRes;
            return Math.min(leftRes, rightRes);
        }

        private Long sequentialSearch(int fromRow, int toRow) {
            long min = Long.MAX_VALUE;
            boolean found = false;
            for (int i = fromRow; i < toRow; i++) {
                for (int j = 0; j < array[0].length; j++) {
                    long val = array[i][j];
                    if (val > target && val < min) {
                        min = val;
                        found = true;
                    }
                }
            }
            return found ? min : null;
        }
    }

    private static Long forkJoinSearch(int[][] array, long target) {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        return pool.invoke(new MinTask(array, 0, array.length, target));
    }

    // ====================== Work Dealing (ExecutorService) ======================
    private static Long executorSearch(int[][] array, long target) throws Exception {
        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        List<Future<Long>> futures = new ArrayList<>();

        int rowsPerThread = array.length / threads;
        for (int i = 0; i < threads; i++) {
            int start = i * rowsPerThread;
            int end = (i == threads - 1) ? array.length : start + rowsPerThread;
            futures.add(exec.submit(() -> sequentialSearch(array, start, end, target)));
        }

        Long globalMin = null;
        for (Future<Long> f : futures) {
            Long res = f.get();
            if (res != null) {
                globalMin = (globalMin == null) ? res : Math.min(globalMin, res);
            }
        }

        exec.shutdown();
        return globalMin;
    }

    private static Long sequentialSearch(int[][] array, int startRow, int endRow, long target) {
        long min = Long.MAX_VALUE;
        boolean found = false;
        for (int i = startRow; i < endRow; i++) {
            for (int j = 0; j < array[0].length; j++) {
                long val = array[i][j];
                if (val > target && val < min) {
                    min = val;
                    found = true;
                }
            }
        }
        return found ? min : null;
    }
}
