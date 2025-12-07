import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Task2_Reviews {

    public static void main(String[] args) {
        System.out.println("=== ЗАВДАННЯ 2: Агрегатор відгуків ===\n");

        long startTime = System.currentTimeMillis();

        CompletableFuture<String> fastAdService = CompletableFuture.supplyAsync(() -> {
            delay(3000); return "AdService_Slow";
        });
        CompletableFuture<String> slowAdService = CompletableFuture.supplyAsync(() -> {
            delay(500); return "AdService_Fast";
        });

        CompletableFuture<Object> firstAd = CompletableFuture.anyOf(fastAdService, slowAdService);
        
        firstAd.thenAccept(result -> 
            System.out.println("[anyOf] Реклама завантажена з: " + result)
        );

        CompletableFuture<Double> amazonRating = getProductBytes("iPhone 15")
                .thenCompose(id -> fetchRating("Amazon", id));

        CompletableFuture<Double> rozetkaRating = fetchRating("Rozetka", 12345);
        CompletableFuture<Double> ebayRating = fetchRating("Ebay", 12345);

        CompletableFuture<Double> combinedSecondary = rozetkaRating.thenCombine(ebayRating, (r1, r2) -> {
            System.out.println("[thenCombine] Об'єднання Rozetka (" + r1 + ") та Ebay (" + r2 + ")");
            return (r1 + r2) / 2.0;
        });

        CompletableFuture<Void> finalReport = amazonRating.thenCombine(combinedSecondary, (amz, othersAvg) -> {
            double finalAvg = (amz + othersAvg) / 2.0;
            return "\n=== ПІДСУМКОВИЙ ЗВІТ ===\n" +
                   "Рейтинг Amazon: " + amz + "\n" +
                   "Середній Rozetka/Ebay: " + othersAvg + "\n" +
                   "ЗАГАЛЬНИЙ РЕЙТИНГ ПРОДУКТУ: " + String.format("%.2f", finalAvg);
        }).thenAccept(System.out::println);

        finalReport.join();
        
        System.out.println("\nПрограма завершена за " + (System.currentTimeMillis() - startTime) + " мс");
    }

    private static CompletableFuture<Integer> getProductBytes(String productName) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("-> Шукаємо ID для " + productName + "...");
            delay(1000);
            return 12345;
        });
    }

    private static CompletableFuture<Double> fetchRating(String source, int productId) {
        return CompletableFuture.supplyAsync(() -> {
            int time = ThreadLocalRandom.current().nextInt(1000, 2500);
            delay(time);
            double rating = 3.5 + ThreadLocalRandom.current().nextDouble() * 1.5;
            System.out.println("-> Отримано рейтинг з " + source + ": " + String.format("%.1f", rating));
            return rating;
        });
    }

    private static void delay(int ms) {
        try { TimeUnit.MILLISECONDS.sleep(ms); } catch (InterruptedException e) { throw new IllegalStateException(e); }
    }
}
