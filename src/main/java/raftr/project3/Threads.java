package raftr.project3;

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
}
