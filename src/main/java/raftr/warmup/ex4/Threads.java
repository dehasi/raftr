package raftr.warmup.ex4;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;

class Threads {

     static void sleep(int seconds) {
        try {
            Thread.sleep(Duration.of(seconds, SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    interface CheckedRunnable {
        void run() throws Exception;
    }

     static Runnable checked(CheckedRunnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
