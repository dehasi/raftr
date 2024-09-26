package raftr.project6;

class Utils {
    @FunctionalInterface
    interface CheckedRunnable {
        void run() throws Exception;
    }


    @FunctionalInterface
    interface CheckedSupplier<V> {
        V get() throws Exception;
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

    static void pizza(CheckedRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static <V> V pizza2(CheckedSupplier<V> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
