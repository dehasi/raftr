package raftr.project1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import static raftr.project1.Message.recvMessage;
import static raftr.project1.Message.sendMessage;

class KVClient {
    public static void main(String[] args) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(System.in));
        var socket = new Socket("localhost", 12345);
        while (true) {
            System.out.print("KV > ");
            String msg = reader.readLine();
            if (msg.isEmpty()) break;
            sendMessage(socket, msg);

            String response = recvMessage(socket);
            System.out.println(response);
        }
        socket.close();
    }
}
