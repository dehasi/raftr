package raftr.project5;

import java.util.Map;

class RaftConfig {
    static final Map<Integer, Server> SERVERS = Map.of(
            1, new Server("localhost", 10001),
            2, new Server("localhost", 10002),
            3, new Server("localhost", 10003)
    );
}
