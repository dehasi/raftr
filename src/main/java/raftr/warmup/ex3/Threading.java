package raftr.warmup.ex3;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;

class Threading {

    private static void sleep(int seconds) {
        try {
            Thread.sleep(Duration.of(seconds, SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static void countdown(int n) {
        while (n > 0) {
            System.out.println("T-mins " + n);
            sleep(1);
            --n;
        }
    }

    static void countup(int stop) {
        int x = 0;
        while (x < stop) {
            System.out.println("Up we go");
            sleep(1);
            ++x;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        var t1 = new Thread(() -> countdown(10));
        var t2 = new Thread(() -> countup(5));
        t1.start();
        t2.start();
        System.out.println("Waiting");
        t1.join();
        t2.join();
        System.out.println("Goodbye");
    }
}
