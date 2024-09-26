package raftr.warmup.ex4;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static raftr.warmup.ex4.Threads.sleep;

public class LocksSolution {

    Lock lock = new ReentrantLock();
    Map<Character, Integer> data = new HashMap<>(){{
        put('x', 1);
        put('y', 2);
        put('z', 3);
    }};

    void processData() {
        lock.lock();
        data.forEach((k,v) -> {
            System.out.printf("%s=%s\n",k,v);
            sleep(1);
        });
        lock.unlock();
    }

    private void run() {
         new Thread(() -> processData()).start();
        sleep(1);
        lock.lock();
        data.put('a', 100);
        lock.unlock();
        System.out.println(data);
    }

    public static void main(String[] args) {
        new LocksSolution().run();
    }
}