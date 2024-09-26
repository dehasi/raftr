package raftr.project8;

import raftr.project8.RaftMessage.SendHeartBeat;
import raftr.project8.RaftMessage.SubmitCommand;
import raftr.project8.RaftNet.RaftNetApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.Integer.parseInt;
import static java.lang.System.out;
import static raftr.project8.RaftConfig.HEARTBEAT_TIMER;
import static raftr.project8.RaftConfig.SERVERS;
import static raftr.project8.Threads.sleep;

class Raft {

    private final RaftLogic raftLogic;
    private final RaftNetApp raftNetApp;
    private final int nodenum;
    private final Function<String, String> stateMachine;

    private final Map<Integer, Consumer<String>> callbacks = new HashMap<>();
    private /*Atomic*/ int transactionId = 42;
    private final Map<Integer, Deffered> futures = new HashMap<>();

    Raft(int nodenum, Function<String, String> stateMachine) {
        this.nodenum = nodenum;
        this.stateMachine = stateMachine;

        raftNetApp = new RaftNetApp(nodenum);
        raftLogic = new RaftLogic(nodenum, SERVERS.size());

        heartbeat();
        mainExecutor();
    }

    private void mainExecutor() {
        new Thread(() -> {
            while (true) {
                var msg = raftNetApp.receive();

                var outgoing = raftLogic.receiveMessage(msg);

                // persist log here

                while (!outgoing.isEmpty())
                    raftNetApp.send(outgoing.poll());

                // If command received from client: append entry to local log,
                // respond after entry applied to state machine (§5.3)
                while (raftLogic.lastApplied < raftLogic.commitIndex) {
                    ++raftLogic.lastApplied;
                    var cmd = raftLogic.log.getEntry(raftLogic.lastApplied).message();
                    applyCommand(cmd);
                }
            }
        }).start();
    }

    private void heartbeat() {
        new Thread(() -> {
            while (true) {
                sleep(HEARTBEAT_TIMER);
                raftNetApp.send(new RaftMessage(nodenum, nodenum, new SendHeartBeat()));
            }
        }).start();
    }

    // (1)
    void submitCommand(String command) {
        if (raftLogic.isLeader())
            raftNetApp.send((new RaftMessage(nodenum, nodenum, new SubmitCommand(command))));
        // what else?
    }

    void submitCommandCallBack(String command, Consumer<String> callback) {
        raftNetApp.send((new RaftMessage(nodenum, nodenum, new SubmitCommand(transactionId + ":" + command))));
        callbacks.put(transactionId, callback);
        ++transactionId;
    }


    Deffered submitCommandDeffered(String command) {
        var future = new Deffered();
        raftNetApp.send((new RaftMessage(nodenum, nodenum, new SubmitCommand(transactionId + ":" + command))));
        futures.put(transactionId, future);
        ++transactionId;
        return future;
    }

    // (3)
    private void applyCommand(String command) {
        out.println("command: " + command + " cb: " + callbacks.keySet() + " ft: " + futures.keySet());
        var split = command.split(":", 2);
        String result = stateMachine.apply(split[1]);
        out.println("APPLIED: {" + split[1] + "}, RESULT: " + result);

        var key = parseInt(split[0]);
        respondToClient(key, result);
    }

    private void respondToClient(int key, String result) {
        if (callbacks.containsKey(key)) {
            out.println("I have a Callback");
            if (raftLogic.isLeader()) {
                out.println("Responding via Callback");
                callbacks.get(key).accept(result);
            } else {
                out.println("I am not a leader anymore, I won't respond");
            }
            callbacks.remove(key);
        }

        if (futures.containsKey(key)) {
            out.println("I have a Future");
            if (raftLogic.isLeader()) {
                out.println("Responding via Future");
                futures.get(key).setResult(result);
            } else {
                out.println("I am not a leader anymore, I won't respond");
            }
            futures.remove(key);
        }
    }

    private void console() throws IOException {
        var reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            out.printf("Raft %d > ", nodenum);
            String msg = reader.readLine();
            if (msg.isEmpty()) continue;

            var split = msg.split("\\s+");
            switch (split[0]) {
                case "leader" -> {
                    raftLogic.becomeLeader();
                }
                case "follow" -> {
                    raftLogic.becameFollower();
                }

                case "status" -> {
                    out.printf("term=%d, role=%s, votedFor=%s\n", raftLogic.currentTerm, raftLogic.role, raftLogic.votedFor);
                }
                case "role" -> {
                    out.println(raftLogic.role);
                }
                case "log" -> {
                    out.println(raftLogic.log.entries);
                }
                case "submit" -> {
                    this.submitCommand(msg.substring("submit ".length()));
                }
                case "many" -> {
                    var count = parseInt(msg.substring("many ".length()));
                    while (count-- > 0) {
                        this.submitCommand("set c " + count);
                    }
                }
            }
        }
    }


    public static void main(String[] args) throws IOException {
        new Raft(parseInt(args[0]), (_) -> "").console();
    }
}
