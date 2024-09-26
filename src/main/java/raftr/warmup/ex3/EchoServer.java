package raftr.warmup.ex3;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static raftr.warmup.ex3.Message.recvMessage;
import static raftr.warmup.ex3.Message.sendMessage;

public class EchoServer {

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.printf("Connection from %s:%s\n", socket.getInetAddress(), socket.getPort());
                    Thread thread = new Thread(() -> echoMessages(socket));
                    thread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    private static void echoMessages(Socket socket) {
        while (true) {
            try {
                var msg = recvMessage(socket);
                sendMessage(socket, msg);
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException ignore) {}
            }
        }
    }
}

