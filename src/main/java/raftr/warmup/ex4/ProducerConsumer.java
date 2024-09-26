package raftr.warmup.ex4;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static raftr.warmup.ex4.Threads.checked;
import static raftr.warmup.ex4.Threads.sleep;

public class ProducerConsumer {

    void producer(Queue<Integer> queue) {
        for (int i = 0; i < 5; i++) {
            System.out.println("Producing " + i);
            queue.add(i);
            sleep(1);
        }
        queue.add(-1);
        System.out.println("Producer done");
    }

    void consumer(BlockingQueue<Integer> queue) throws InterruptedException {
        System.out.println("Consumer start");
        while (true) {
            int item = queue.take();
            if (item < 0) break;
            System.out.println("Consuming " + item);
        }
        System.out.println("Consumer goodbye!");
    }

    private void run() throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(15);
        var t1 = new Thread(() -> producer(queue));
        var t2 = new Thread(checked(() -> consumer(queue)));
        t1.start();
        t2.start();
        System.out.println("Waiting");
        t1.join();
        t2.join();
        System.out.println("Done");
    }


    public static void main(String[] args) throws InterruptedException {
        new ProducerConsumer().run();
    }
}