package raftr.project3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import static raftr.project3.Message.recvMessage;
import static raftr.project3.Message.sendMessage;
import static raftr.project3.RaftKVServer.KVSERVERS;

class RaftKVClient {
    public static void main(String[] args) throws IOException {
        int serverno = Integer.parseInt(args[1]);
        var server = KVSERVERS.get(serverno);
        var socket = new Socket(server.host(), server.port());

        var reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.printf("KV %d > ", serverno);
            String msg = reader.readLine();
            if (msg.isEmpty()) break;
            sendMessage(socket, msg);

            String response = recvMessage(socket);
            System.out.println(response);
        }
        socket.close();
    }
}
