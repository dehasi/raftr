package raftr.warmup.ex1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

class TimeClient {
    public static void main(String[] args) throws IOException {

        Socket socket = new Socket("localhost", 12345);
        var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println(in.readLine());
        socket.close();
    }
}
