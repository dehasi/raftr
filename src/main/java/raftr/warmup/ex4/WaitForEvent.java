package raftr.warmup.ex4;

import static raftr.warmup.ex4.Threads.checked;
import static raftr.warmup.ex4.Threads.sleep;

class WaitForEvent {

    void waiter(Object evt) throws InterruptedException {
        System.out.println("I am waiting");
        synchronized (evt) {
            evt.wait();
        }
        System.out.println("I am awake");
    }

    private void run() {
        var evt = new Object();
        for (int i = 0; i < 5; i++)
            new Thread(checked(() -> waiter(evt))).start();
        sleep(3);
        synchronized (evt) {
            evt.notifyAll();
        }
    }

    public static void main(String[] args) {
        new WaitForEvent().run();
    }
}
