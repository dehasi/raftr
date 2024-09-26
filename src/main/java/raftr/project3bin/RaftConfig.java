package raftr.project3bin;

import java.util.Map;

class RaftConfig {
    static final Map<Integer, Server> SERVERS = Map.of(
            1, new Server("localhost", 12001),
            2, new Server("localhost", 12002),
            3, new Server("localhost", 12003)
    );
}
