package raftr.project5;

class DifferedResult {
    String value;

    void setResult(String value) {
        this.value = value;
        synchronized (this) {
            this.notifyAll();
        }
    }

    String result() {
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
