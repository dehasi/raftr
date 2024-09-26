package raftr.warmup.ex3;

import java.io.IOException;
import java.net.Socket;

import static java.lang.Integer.parseInt;
import static java.lang.System.arraycopy;
import static java.nio.charset.StandardCharsets.UTF_8;

class Message {

    // We use text protocol, we use UTF-8
    // size in utf-8 message of 10 bytes leng => 10 digits
    static final int MSG_SIZE_SYMBOLS = 10; // as msg bytes => 10 symbols 1_000_000_000

    static String recvExactly(Socket socket, int nbytes) throws IOException {
        byte[] message = new byte[nbytes];
        int shift = 0;
        while (nbytes > 0) {
            var chunk = socket.getInputStream().readNBytes(nbytes);
            if (chunk.length == 0) throw new IOException("Incomplete message");

            arraycopy(chunk, 0, message, shift, chunk.length);
            shift += chunk.length;
            nbytes -= chunk.length;
        }

        return new String(message);
    }

    static String recvMessage(Socket socket) throws IOException {
        String len = recvExactly(socket, MSG_SIZE_SYMBOLS).trim();
        return recvExactly(socket, parseInt(len));
    }

    static void sendMessage(Socket socket, String message) throws IOException {
        byte[] messageBytes = message.getBytes(UTF_8);
        var len = String.format("%10d", messageBytes.length);

        socket.getOutputStream().write(len.getBytes(UTF_8));
        socket.getOutputStream().write(messageBytes);
    }
}
