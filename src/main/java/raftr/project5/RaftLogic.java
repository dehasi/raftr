package raftr.project5;

import raftr.project5.RaftMessage.AppendEntries;
import raftr.project5.RaftMessage.AppendEntriesResponse;
import raftr.project5.RaftNet.RaftNetApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Math.max;
import static raftr.project5.RaftLogic.Role.FOLLOWER;
import static raftr.project5.RaftLogic.Role.LEADER;

class RaftLogic {

    private final List<Integer> neigbours;
    private final int myid;
    private final RaftNetApp raftNet;
    private final int quorum;

    RaftLogic(int myid, List<Integer> nodeids, RaftNetApp raftNet) {
        this.myid = myid;
        this.quorum = 1 + nodeids.size() / 2;
        ArrayList<Integer> tmp = new ArrayList<>(nodeids);
        tmp.removeIf(id -> id == myid);
        this.neigbours = tmp;
        this.raftNet = raftNet;
    }

    void startHeartBeat() {
        for (var n : neigbours) {
            var hb = new Thread(() -> heartbeat(n));
            hb.setName("HeatBeat" + n);
            hb.start();
        }
    }

    private void heartbeat(int n) {
        while (!Thread.interrupted()) {
            Threads.sleep(1);
            if (role == LEADER) {
                try {
                    sendAppendEntries(n);
                } catch (Exception e) {e.printStackTrace();}
            } else break;
        }
    }

    enum Role {
        LEADER, FOLLOWER, CANDIDATE
    }

    private volatile Role role = FOLLOWER;

    // -- Figure 2
    // Persistent state on all servers
    private int currentTerm = 0;
    private Integer votedFor = null;
    private RaftLog log = new RaftLog();

    // Volatile state on all servers
    private volatile int commitIndex = 0;
    private volatile int lastApplied = 0;

    //  Volatile state on leader
    private final Map<Integer, Integer> nextIndex = new HashMap<>();
    private final Map<Integer, Integer> matchIndex = new HashMap<>();

    BlockingQueue<String> toApply = new ArrayBlockingQueue<>(100500);
    Map<Integer, DifferedResult> result = new ConcurrentHashMap<>();
    // eventually invoke via election
    void becomeLeader() {
        role = LEADER;
        // ++currentTerm; // do I need it?
        for (var nb : neigbours) {
            nextIndex.put(nb, log.highest_index + 1);
            matchIndex.put(nb, 0);
        }
    }

    DifferedResult submitNewCommand(String command) {
        assert role == LEADER;
        log.appendNewCommand(currentTerm, command);
        DifferedResult future = new DifferedResult();
        result.put(log.highest_index, future)        ;
        return future;
    }

    void sendAppendEntries(int follower) {
        assert role == LEADER;
        int prevLogIndex = nextIndex.get(follower) - 1;
        var entries = log.getEntries(prevLogIndex, log.highest_index);
        int prevLogTerm = entries.getFirst().term();
        entries = new ArrayList<>(entries.subList(1, entries.size()));

        var appendEntries = new AppendEntries(
                currentTerm, prevLogIndex, prevLogTerm, entries, commitIndex); // mcommitIndex   |-> Min({commitIndex[i], lastEntry}),

        raftNet.send(follower, new RaftMessage(myid, follower, appendEntries));
    }

    void receiveAppendEntries(int leader, AppendEntries msg) {
        assert role == FOLLOWER; // will be refine
        System.err.printf("receiveAppendEntries(%d, %s)\n", leader, msg);

        if (msg.term() < currentTerm)
            raftNet.send(leader, new RaftMessage(myid, leader, new AppendEntriesResponse(currentTerm, false, -1)));

        var result = log.appendEntries(msg.prevLogIndex(), msg.prevLogTerm(), msg.entries());
        if (!result)
            raftNet.send(leader, new RaftMessage(myid, leader, new AppendEntriesResponse(currentTerm, false, -1)));

        if (msg.leaderCommit() > commitIndex)
            commitIndex = Math.min(msg.leaderCommit(), log.highest_index);
        raftNet.send(leader, new RaftMessage(myid, leader,
                new AppendEntriesResponse(currentTerm, true, msg.prevLogIndex() + msg.entries().size())));
    }

    void receiveAppendEntriesResponse(int follower, AppendEntriesResponse msg) {
        assert role == LEADER;
        System.err.printf("receiveAppendEntriesResponse(%d, %s)\n", follower, msg);

        if (msg.success()) {
            nextIndex.put(follower, msg.matchIndex() + 1);
            matchIndex.put(follower, msg.matchIndex());
        } else
            nextIndex.put(follower, max(nextIndex.get(follower) - 1, 1));

        advanceCommitIndex();
    }

    void advanceCommitIndex() {
        assert role == LEADER;
        Map<Integer, Integer> inxToCount = new HashMap<>();
        matchIndex.values().forEach(index -> inxToCount.put(index, inxToCount.getOrDefault(index, 0) + 1));

        int newCommitIndex = commitIndex;
        for (var kv : inxToCount.entrySet()) {
            int index = kv.getKey(), count = kv.getValue();
            if (count >= quorum-1) {
                newCommitIndex = max(newCommitIndex, index);
            }
        }
        commitIndex = newCommitIndex;
    }

    private void commit() {
        if (lastApplied >= commitIndex) return;
        // copy for comcurrenyy
        int applied = lastApplied, commited = commitIndex;
        List<RaftLog.LogEntry> entries = log.getEntries(applied, commited);
//        entries.stream()
//                .map(RaftLog.LogEntry::message)
//                .forEach(toApply::add);
        for(int i = 0; i < entries.size(); i++){
            int entryIdx = i+applied;
            result.get(entryIdx).setResult(entries.get(i).message());
        }
    }
}
