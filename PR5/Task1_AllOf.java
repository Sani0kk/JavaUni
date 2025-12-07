import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;

public class Task1_AllOf {
    public static void main(String[] args) {
        System.out.println("=== ЗАВДАННЯ 1: Демонстрація allOf() ===");

        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            final int taskId = i;
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                int delay = ThreadLocalRandom.current().nextInt(1, 4);
                try {
                    TimeUnit.SECONDS.sleep(delay);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
                return "Задача " + taskId + " (час виконання: " + delay + "с)";
            });
            futures.add(future);
        }

        CompletableFuture<Void> allDone = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        allDone.thenRun(() -> {
            System.out.println("\n>>> Всі задачі завершено успішно!");
            
            String combinedResult = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.joining("\n"));
            
            System.out.println("Результати виконання:\n" + combinedResult);
        }).join(); 
    }
}
