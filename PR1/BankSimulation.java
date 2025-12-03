import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class BankSimulation {
    public static void main(String[] args) {
        Bank bank = new Bank(3);

        Thread scheduler = new Thread(new BankScheduler(bank, 30_000));
        scheduler.start();

        for (int i = 1; i <= 15; i++) {
            Thread clientThread = new Thread(new Client(bank, i), "Клієнт-" + i);
            clientThread.start();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        try {
            scheduler.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("\nСимуляція завершена.");
    }
}

class Bank {
    private final Semaphore atms;
    private final AtomicBoolean isOpen = new AtomicBoolean(true);

    public Bank(int atmCount) {
        this.atms = new Semaphore(atmCount, true);
    }

    public boolean isOpen() {
        return isOpen.get();
    }

    public void closeBank() {
        isOpen.set(false);
        System.out.println("\n=== БАНК ЗАЧИНЯЄТЬСЯ! Банкомати більше недоступні ===\n");
    }

    public boolean acquireATM(int clientId) {
        while (true) {
            if (!isOpen.get()) {
                System.out.println("Клієнт " + clientId + " не встиг — банк уже зачинений.");
                return false;
            }
            if (atms.tryAcquire()) {
                if (!isOpen.get()) {
                    atms.release();
                    System.out.println("Клієнт " + clientId + " не встиг — банк зачинився.");
                    return false;
                }
                return true;
            }
            try {
                Thread.sleep(1000);
                System.out.println("Клієнт " + clientId + " чекає банкомат... (вільно: " + atms.availablePermits() + ")");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    public void useATM(int clientId) {
        System.out.println("Клієнт " + clientId + " намагається зайняти банкомат.");
        if (!acquireATM(clientId)) {
            return;
        }
        try {
            System.out.println(">>> Клієнт " + clientId + " обслуговується в банкоматі");
            Thread.sleep((long) (Math.random() * 6000 + 4000)); // 4–10 сек
        } catch (InterruptedException e) {
            System.out.println("Клієнт " + clientId + " перерваний!");
            Thread.currentThread().interrupt();
        } finally {
            atms.release();
            System.out.println("Клієнт " + clientId + " пішов. Банкомат звільнено.\n");
        }
    }
}

class Client implements Runnable {
    private final Bank bank;
    private final int id;

    public Client(Bank bank, int id) {
        this.bank = bank;
        this.id = id;
    }

    @Override
    public void run() {
        System.out.println("\nКлієнт " + id + " прибув до банку.");
        if (!bank.isOpen()) {
            System.out.println("Клієнт " + id + " розвернувся — банк зачинений.");
            return;
        }
        bank.useATM(id);
    }
}

class BankScheduler implements Runnable {
    private final Bank bank;
    private final long openTimeMs;

    public BankScheduler(Bank bank, long openTimeMs) {
        this.bank = bank;
        this.openTimeMs = openTimeMs;
    }

    @Override
    public void run() {
        System.out.println("=== БАНК ВІДКРИТО! Робочий день розпочато ===\n");
        try {
            Thread.sleep(openTimeMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        bank.closeBank();
    }
}
