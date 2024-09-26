package raftr.warmup.ex1;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;

public class TimeServer {

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(12345);
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                System.out.printf("Connection from %s:%s\n", socket.getInetAddress(), socket.getPort());
                PrintStream stream = new PrintStream(socket.getOutputStream());
                stream.println(LocalDateTime.now());
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

