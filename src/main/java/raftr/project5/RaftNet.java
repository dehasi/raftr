package raftr.project5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import static java.lang.Integer.parseInt;
import static raftr.project5.MessageUtils.recvMessageBin;
import static raftr.project5.MessageUtils.sendMessageBin;
import static raftr.project5.RaftConfig.SERVERS;

public class RaftNet {

    static class RaftNetApp {
        final int nodenum;
        final ServerSocket serverSocket;

        public RaftNetApp(int nodenum, ServerSocket serverSocket) {
            // for tests
            this.nodenum = nodenum;
            this.serverSocket = serverSocket;
        }

        RaftNetApp(int nodenum) {
            this.nodenum = nodenum;
            try {

                Server server = SERVERS.get(nodenum);
                System.err.println( "nodenum " + nodenum +  ", Becoming: " + server);
                serverSocket = new ServerSocket(server.port());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        void send(int destination, RaftMessage msg) {
            var server = SERVERS.get(msg.dst());
            try (var socket = new Socket(server.host(), server.port())) {
                // maybe it's not sender responsibility to format a message
                System.err.println("RaftNet send: " + msg);
                sendMessageBin(socket, msg);
            } catch (Exception ignore) {}
        }

        RaftMessage receive() {
            try (Socket socket = serverSocket.accept()) {
                RaftMessage msg = (RaftMessage) recvMessageBin(socket);
                System.err.println("RaftNet received: " + msg);
                return msg;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // this code won't be used I guess
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

            raftNetApp.send(parseInt(split[0]), null);
        }
    }

    public static void main(String[] args) throws IOException {
        new RaftNet().run(parseInt(args[0]));
    }
}