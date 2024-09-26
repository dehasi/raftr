package raftr.project7;

import raftr.project7.RaftMessage.SendHeartBeat;
import raftr.project7.RaftNet.RaftNetApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static raftr.project7.RaftConfig.HEARTBEAT_TIMER;
import static raftr.project7.RaftConfig.SERVERS;
import static raftr.project7.Threads.sleep;

class Raft {

    RaftLogic raftLogic;
    RaftNetApp raftNetApp;
    final int nodenum;

    Raft(int nodenum) {
        this.nodenum = nodenum;

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
        raftNetApp.send((new RaftMessage(nodenum, nodenum, new RaftMessage.SubmitCommand(command))));
        // raftLogic.submitNewCommand(command);
    }

    // (3)
    void applyCommand(String command) {
        System.out.println("APPLIED: " + command);
    }

    private void console() throws IOException {
        var reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.printf("Raft %d > ", nodenum);
            String msg = reader.readLine();
            if (msg.isEmpty()) continue;

            var split = msg.split("\\s+");
            switch (split[0]) {
                case "leader"-> {
                    raftLogic.becomeLeader();
                }
                case "log" -> {
                    System.out.println(raftLogic.log.entries);
                }
                case "submit" -> {
                    this.submitCommand(msg.substring("submit ".length()));
                }
                case "many" -> {
                    var count = Integer.parseInt(msg.substring("many ".length()));
                    while (count-- > 0) {
                        this.submitCommand("set c " + count);
                    }
                }
            }
        }
    }


    public static void main(String[] args) throws IOException {
        new Raft(Integer.parseInt(args[0])).console();
    }
}
