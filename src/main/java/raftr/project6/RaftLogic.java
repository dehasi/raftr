package raftr.project6;

import raftr.project6.RaftMessage.AppendEntries;
import raftr.project6.RaftMessage.AppendEntriesResponse;
import raftr.project6.RaftMessage.RequestVote;
import raftr.project6.RaftMessage.RequestVoteResponse;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Math.max;
import static java.util.Collections.sort;
import static raftr.project6.RaftConfig.ELECTION_TIMER;
import static raftr.project6.RaftLogic.Role.*;

class RaftLogic {

    private final int clusterSize;
    private final int myid;

    RaftLogic(int myid, int clusterSize) {
        this.myid = myid;
        this.clusterSize = clusterSize;
    }

    boolean isLeader() {
        return role == Role.LEADER;
    }

    enum Role {
        LEADER, FOLLOWER, CANDIDATE
    }

    volatile Role role = FOLLOWER;

    // -- Figure 2
    // Persistent state on all servers
    private int currentTerm = 0;
    private Integer votedFor = null;
    RaftLog log = new RaftLog();

    // Volatile state on all servers
    int commitIndex = 0;
    int lastApplied = 0;

    //  Volatile state on leader
    private final Map<Integer, Integer> nextIndex = new HashMap<>();
    private final Map<Integer, Integer> matchIndex = new HashMap<>();

    BlockingQueue<String> toApply = new ArrayBlockingQueue<>(100500);
    Map<Integer, DifferedResult> result = new ConcurrentHashMap<>();

    // eventually invoke via election
    void becomeLeader() {
        role = LEADER;
        // ++currentTerm; // do I need it?
        for (int node = 1; node <= clusterSize; ++node) {
            nextIndex.put(node, log.highest_index + 1);
            matchIndex.put(node, 0);
        }
    }

    Queue<RaftMessage> receiveMessage(RaftMessage msg) {
        Queue<RaftMessage> response = new LinkedList<>();

        if (msg.isSubmitCommand())
            submitNewCommand(msg.asSubmitCommand().cmd());
        else if (msg.isAppendEntries())
            receiveAppendEntries(msg.src(), msg.asAppendEntries(), response);
        else if (msg.isAppendEntriesResponse())
            receiveAppendEntriesResponse(msg.src(), msg.asAppendEntriesResponse());
        else if (msg.isRequestVote())
            receiveRequestVote(msg.src(), msg.asRequestVote(), response);
        else if (msg.isRequestVoteResponse())
            receiveRequestVoteResponse(msg.src(), msg.asRequestVoteResponse(), response);
        else if (msg.isSendHeartBeat())
            heartBeat(response);

        else throw new RuntimeException("I don't know how to process: " + msg);
        return response;
    }

    private void submitNewCommand(String command) {
        assert role == LEADER;
        log.appendNewCommand(currentTerm, command);
        matchIndex.put(myid, log.highest_index);
    }

    int timer = 0;

    private void heartBeat(Queue<RaftMessage> response) {
        switch (role) {
            case LEADER -> {
                for (int node = 1; node <= clusterSize; ++node)
                    if (node != myid) sendAppendEntries(node, response);

            }
            case CANDIDATE -> {
                System.out.println("VOTING VOTING!");
            }
            case FOLLOWER -> {
                ++timer;
                if (timer >= ELECTION_TIMER) {
                    role = CANDIDATE;
                    ++currentTerm;
                    for (int node = 1; node <= clusterSize; ++node)
                        if (node != myid) response.add(new RaftMessage(myid, node, new RequestVote()));
                }
            }

        }
    }

    private void sendAppendEntries(int follower, Queue<RaftMessage> response) {
        assert role == LEADER;
        int prevLogIndex = nextIndex.get(follower) - 1;
        var entries = log.getEntries(prevLogIndex, log.highest_index);
        int prevLogTerm = entries.getFirst().term();
        entries = new ArrayList<>(entries.subList(1, entries.size()));

        var appendEntries = new AppendEntries(
                currentTerm, prevLogIndex, prevLogTerm, entries, commitIndex); // mcommitIndex   |-> Min({commitIndex[i], lastEntry}),

        response.add(new RaftMessage(myid, follower, appendEntries));
    }


    private void receiveAppendEntries(int leader, AppendEntries msg, Queue<RaftMessage> response) {
        assert role == FOLLOWER; // will be refine
        timer = 0;
        System.err.printf("receiveAppendEntries(%d, %s)\n", leader, msg);

        if (msg.term() < currentTerm) {
            response.add(new RaftMessage(myid, leader, new AppendEntriesResponse(currentTerm, false, -1)));
            return;
        }
        var result = log.appendEntries(msg.prevLogIndex(), msg.prevLogTerm(), msg.entries());
        if (!result) {
            response.add(new RaftMessage(myid, leader, new AppendEntriesResponse(currentTerm, false, -1)));
            return;
        }

        if (msg.leaderCommit() > commitIndex)
            commitIndex = Math.min(msg.leaderCommit(), log.highest_index);
        response.add(new RaftMessage(myid, leader,
                new AppendEntriesResponse(currentTerm, true, msg.prevLogIndex() + msg.entries().size())));
    }

    private void receiveAppendEntriesResponse(int follower, AppendEntriesResponse msg) {
        assert role == LEADER;
        System.err.printf("receiveAppendEntriesResponse(%d, %s)\n", follower, msg);

        if (msg.success()) {
            nextIndex.put(follower, msg.matchIndex() + 1);
            matchIndex.put(follower, msg.matchIndex());
            advanceCommitIndex();
        } else
            nextIndex.put(follower, max(nextIndex.get(follower) - 1, 1)); // maybe add defencive programming
    }

    private void advanceCommitIndex() {
        assert role == LEADER;
        var values = new ArrayList<>(this.matchIndex.values());
        sort(values);

        int newCommitIndex = values.get(clusterSize / 2);

        commitIndex = Math.max(commitIndex, newCommitIndex);
    }


    private void receiveRequestVote(int from, RequestVote msg, Queue<RaftMessage> response) {}

    private void receiveRequestVoteResponse(int from, RequestVoteResponse msg, Queue<RaftMessage> response) {}
}
