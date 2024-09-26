package raftr.warmup.ex4;

public class CommunicateSingleResult {

    static class MyFuture<Value> {
        Value value;

        void setResult(Value value) {
            this.value = value;
            synchronized (this) {
                this.notifyAll();
            }
        }

        Value result() {
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return value;
        }
    }

    void f(int x, int y, MyFuture<Integer> fut) {
        Threads.sleep(5);
        fut.setResult(x+y);
    }

    private void run() {
        var fut = new MyFuture<Integer>();
        new Thread(() -> f(2, 3, fut)).start();
        System.out.println(fut.result());
    }

    public static void main(String[] args) {
        new CommunicateSingleResult().run();
    }
}