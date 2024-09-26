package raftr.project5;

import raftr.project5.RaftNet.RaftNetApp;

import java.util.ArrayList;

import static raftr.project5.RaftConfig.SERVERS;

class Raft {

    RaftLogic raftLogic;
    RaftNetApp raftNetApp;
    final int nodenum;

    Raft(int nodenum) {
        this.nodenum = nodenum;

        raftNetApp = new RaftNetApp(nodenum);
        raftLogic = new RaftLogic(nodenum, new ArrayList<>(SERVERS.keySet()), raftNetApp);
        if (nodenum == 1)
            raftLogic.becomeLeader();
        raftLogic.startHeartBeat();

        new Thread(() -> {
            while (true) {
                var msg = raftNetApp.receive();
                if (msg.isAppendEntries())
                    raftLogic.receiveAppendEntries(msg.src(), msg.asAppendEntries());
                else if (msg.isAppendEntriesResponse())
                    raftLogic.receiveAppendEntriesResponse(msg.src(), msg.asAppendEntriesResponse());
                else throw new RuntimeException("I don't know how to process: " + msg);
            }
        }).start();

    }

    // (1)
    void submitCommand(String command) {
        raftLogic.submitNewCommand(command);
    }

    // (3)
    void applyCommand(String command) {}

    private void run() {

    }


    public static void main(String[] args) {
        new Raft(3).run();
    }
}
