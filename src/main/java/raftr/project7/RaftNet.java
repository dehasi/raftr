package raftr.project7;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static raftr.project7.MessageUtils.recvMessageBin;
import static raftr.project7.MessageUtils.sendMessageBin;
import static raftr.project7.RaftConfig.SERVERS;

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
                System.err.println("nodenum " + nodenum + ", Becoming: " + server);
                serverSocket = new ServerSocket(server.port());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        void send(RaftMessage msg) {
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
}