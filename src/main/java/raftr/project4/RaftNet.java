package raftr.project4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static raftr.project4.MessageUtils.recvMessage;
import static raftr.project4.MessageUtils.sendMessage;
import static raftr.project4.RaftConfig.SERVERS;

public class RaftNet {


    static class RaftNetApp {
        final int nodenum;
        final ServerSocket serverSocket;

        RaftNetApp(int nodenum) {
            this.nodenum = nodenum;
            try {
                serverSocket = new ServerSocket(SERVERS.get(nodenum).port());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        void send(int destination, String msg) {
            var server = SERVERS.get(destination);
            try (var socket = new Socket(server.host(), server.port())) {
                // maybe it's not sender responsibility to format a message
                sendMessage(socket, format("{from=%d, to=%d, msg=%s}", this.nodenum, destination, msg));
            } catch (Exception ignore) {}
        }

        String receive() {
            try (Socket socket = serverSocket.accept()) {
                return recvMessage(socket);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void run(int id) throws IOException {
        System.out.println(id);
        var raftNetApp = new RaftNetApp(id);
        new Thread(() -> {
            while (true) {
                var msg = raftNetApp.receive();
                System.out.println(msg);
            }
        }).start();

        var reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.printf("Node %d > ", id);
            String msg = reader.readLine();
            if (msg.isEmpty()) break;
            String[] split = msg.split("\\s", 2);

            raftNetApp.send(parseInt(split[0]), split[1]);
        }
    }

    public static void main(String[] args) throws IOException {
        new RaftNet().run(parseInt(args[0]));
    }
}