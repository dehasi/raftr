package raftr.project4;

import raftr.project4.RaftMessage.AppendEntries;
import raftr.project4.RaftMessage.AppendEntriesResponse;

import java.util.List;

class Raft {


    final int nodenum;

    Raft(int nodenum) {this.nodenum = nodenum;}

    static class RaftApp {
        // persistent
        int currentTerm;
        int votedFor;
        List<String> log;

        // all
        int commitIndex;
        int lastApplied;

        // leader
        int nextIndex; // should be map {node:nextIndex}
        int matchIndex; // should be map {node:nextIndex}


        AppendEntriesResponse handleCommand(AppendEntries appendEntries) {

            return null;
        }
    }

    // (1)
    void submitCommand(String command) {
    }

    // (3)
    void applyCommand(String command) {}

    private void run() {

    }

    public static void main(String[] args) {
        new Raft(3).run();
    }
}
