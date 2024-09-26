package raftr.warmup.ex3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import static raftr.warmup.ex3.Message.recvMessage;
import static raftr.warmup.ex3.Message.sendMessage;

class EchoClient {
    public static void main(String[] args) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(System.in));
        var socket = new Socket("localhost", 12345);

        while (true) {
            System.out.print("Say>");
            String msg = reader.readLine();
            if (msg.isEmpty()) break;

            sendMessage(socket, msg);
            String response = recvMessage(socket);
            System.out.println("Received >" + response);
        }
        socket.close();
    }
}
