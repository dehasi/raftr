package raftr.warmup.ex4;

import java.util.HashMap;
import java.util.Map;

import static raftr.warmup.ex4.Threads.sleep;

public class LocksProblem {

    Map<Character, Integer> data = new HashMap<>(){{
        put('x', 1);
        put('y', 2);
        put('z', 3);
    }};

    void processData() {
        data.forEach((k,v) -> {
            System.out.printf("%s=%s\n",k,v);
            sleep(1);
        });
    }

    private void run() {
         new Thread(() -> processData()).start();
        sleep(1);
        data.put('a', 100);
        System.out.println(data);
    }

    public static void main(String[] args) {
        new LocksProblem().run();
    }
}