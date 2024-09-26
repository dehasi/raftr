package raftr.project2;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static java.nio.charset.StandardCharsets.UTF_8;

class Traffic {

    static void sendUDP(int port, String message) {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = message.getBytes(UTF_8);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("127.0.0.1"), port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void listenUPD(int port, Queue<String> queue) {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            while (true) {
                byte[] buffer = new byte[1024];

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String event = new String(packet.getData(), 0, packet.getLength());
                queue.add(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void tick(int second, Queue<String> queue) {
        while (true) {
            Threads.sleep(second);
            queue.add("tick");
        }
    }

    private void run() throws InterruptedException {
        TrafficApp trafficApp = new TrafficApp();
        final BlockingQueue<String> queue = new ArrayBlockingQueue<>(150);
        new Thread(() -> listenUPD(20000, queue)).start();
        new Thread(() -> tick(1, queue)).start();

        final int EW = 10000;
        final int NS = 10001;
        while (true) {
            String event = queue.take();
            trafficApp.handleEvent(event);

            sendUDP(EW, trafficApp.EW + "");
            sendUDP(NS, trafficApp.NS + "");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new Traffic().run();
    }

    static class TrafficApp {
        private char NS = 'R', EW = 'R';
        private int clock = 0;
        private volatile boolean nsPressed = false;
        private volatile boolean ewPressed = false;

        char[] changeLights() {
            if (clock > 0) return null;
            switch (NS + "" + EW) {
                case "GR" -> {
                    clock = 5;
                    nsPressed = ewPressed = false;
                    return new char[]{NS = 'Y', EW = 'R'};
                }
                case "YR" -> {
                    clock = 60;
                    return new char[]{NS = 'R', EW = 'G'};
                }
                case "RG" -> {
                    clock = 5;
                    nsPressed = ewPressed = false;
                    return new char[]{NS = 'R', EW = 'Y'};
                }
                case "RY", "RR" -> {
                    clock = 30;
                    return new char[]{NS = 'G', EW = 'R'};
                }
                case null, default -> {return null;}
            }
        }

        void handleEvent(String event) {
            System.out.println("handle: " + event);

            switch (event) {
                case "EW" -> {
                    if (ewPressed) break;
                    ewPressed = true;
                    clock -= 15;
                }
                case "NS" -> {
                    if (nsPressed) break;
                    nsPressed = true;
                    clock -= 3 * 15;
                }
                // maybe courting up would be better
                case "tick" -> {
                    --clock;
                }
            }
            changeLights();
            // check invariants
        }
    }
}
