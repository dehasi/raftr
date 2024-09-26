package raftr.warmup.ex2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static raftr.warmup.ex2.Message.recvMessage;
import static raftr.warmup.ex2.Message.sendMessage;

public class EchoServer {

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.printf("Connection from %s:%s\n", socket.getInetAddress(), socket.getPort());
                    echoMessages(socket);
                } catch (IOException e) {
                    e.printStackTrace();
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
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }
}

