package raftr.project1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static raftr.project1.Message.recvMessage;
import static raftr.project1.Message.sendMessage;
import static raftr.project1.Utils.pizza;
import static raftr.project1.Utils.pizza2;

/*

My KV Server Restrictions:
* Commands: only lowercase.
* Keys: only a word (no spaces).
* Values: only a word (no spaces).
*/
public class KVServer {

    Lock lock = new ReentrantLock();
    KVApp kv = new KVApp();

    private void run() throws IOException {
        int port = 12345;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("KV Server listening on port " + port);
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.printf("Connection from %s:%s\n", socket.getInetAddress(), socket.getPort());
                    Thread thread = new Thread(() -> handleCommandlines(socket));
                    thread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    private void handleCommandlines(Socket socket) {
        while (true) {
            try {
                var msg = recvMessage(socket);
                lock.lock();
                String response = kv.runCommand(msg);
                lock.unlock();
                sendMessage(socket, response);
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException ignore) {}
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new KVServer().run();
    }

    static class KVApp {
        private Map<String, String> data = new HashMap<>();
        private List<String> log = new ArrayList<>();

        String runCommand(String msg) {
            var split = msg.split("\\s");
            switch (split[0]) {
                case "delete" -> {
                    assert split.length == 2;
                    log.add(msg);
                    data.remove(split[1]);
                    return "ok";
                }
                case "get" -> {
                    assert split.length == 2;
                    return data.getOrDefault(split[1], "");
                }
                case "set" -> {
                    assert split.length == 3;
                    log.add(msg);
                    data.put(split[1], split[2]);
                    return "ok";
                }
                case "snapshot" -> {
                    assert split.length == 2;
                    saveLog(log, split[1]);
                    return "ok";
                }
                case "restore" -> {
                    assert split.length == 2;
                    data = restoreSnapshotFrom(log = restoreLog(split[1]));
                    return "ok";
                }
                case null, default -> {
                    return "badcommand";
                }
            }
        }

        private static void saveLog(List<String> log, String name) {
            Path path = snapshotFor(name);
            String content = String.join("\n", log);
            pizza(() -> Files.writeString(path, content));
        }

        private static Map<String, String> restoreSnapshotFrom(List<String> log) {
            Map<String, String> map = new HashMap<>();
            for (var entry : log) {
                var split = entry.split("\\s");
                switch (split[0]) {
                    case "delete" -> map.remove(split[1]);
                    case "set" -> map.put(split[1], split[2]);
                }
            }
            return map;
        }

        private static List<String> restoreLog(String name) {
            Path path = snapshotFor(name);
            return new ArrayList<>(pizza2(() -> Files.lines(path).toList()));
        }

        private static Path snapshotFor(String name) {
            return Path.of(name + ".snapshot");
        }

        // not used so far, maybe will he helpful for log compaction
        private static void saveSnapshot(Map<String, String> map, String name) {
            Path path = snapshotFor(name);
            String content = map.entrySet().stream()
                    .map(entry -> entry.getKey() + " " + entry.getValue())
                    .collect(joining("\n"));
            pizza(() -> Files.writeString(path, content));
        }

        private static Map<String, String> restoreSnapshot(String name) {
            Path path = snapshotFor(name);

            Map<String, String> map = pizza2(() -> Files.lines(path)
                    .map(line -> line.split(" "))
                    .collect(toMap(kv -> kv[0].trim(), kv -> kv[1].trim())));
            return new HashMap<>(map);
        }
    }
}

