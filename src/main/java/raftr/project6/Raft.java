package raftr.project6;

import raftr.project6.RaftMessage.SendHeartBeat;
import raftr.project6.RaftNet.RaftNetApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static raftr.project6.RaftConfig.HEARTBEAT_TIMER;
import static raftr.project6.RaftConfig.SERVERS;
import static raftr.project6.Threads.sleep;

class Raft {

    RaftLogic raftLogic;
    RaftNetApp raftNetApp;
    final int nodenum;
    BlockingQueue<RaftMessage> queue = new ArrayBlockingQueue<>(1500);

    Raft(int nodenum) {
        this.nodenum = nodenum;

        raftNetApp = new RaftNetApp(nodenum);
        raftLogic = new RaftLogic(nodenum, SERVERS.size());
        if (nodenum == 1)
            raftLogic.becomeLeader();

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
                case "log" -> {
                    System.out.println(raftLogic.log.entries);
                }
                case "submit" -> {
                    this.submitCommand(msg.substring("submit".length()));
                }
            }
        }
    }


    public static void main(String[] args) throws IOException {
        new Raft(Integer.parseInt(args[0])).console();
    }
}
