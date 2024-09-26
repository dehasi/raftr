package raftr.project8;

import java.util.Map;

class RaftConfig {
    static final Map<Integer, Server> SERVERS = Map.of(
            1, new Server("localhost", 10001),
            2, new Server("localhost", 10002),
            3, new Server("localhost", 10003)
    );

    static final int HEARTBEAT_TIMER = 1; // second
    static final int ELECTION_TIMER = 10 * HEARTBEAT_TIMER;
}
