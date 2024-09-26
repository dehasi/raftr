package raftr.project8;

import raftr.project8.RaftMessage.AppendEntries;
import raftr.project8.RaftMessage.AppendEntriesResponse;
import raftr.project8.RaftMessage.RequestVote;
import raftr.project8.RaftMessage.RequestVoteResponse;

import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Collections.sort;
import static raftr.project8.RaftLogic.Role.*;
import static raftr.project8.RaftConfig.ELECTION_TIMER;

class RaftLogic {

    private final int clusterSize;
    private final int myid;

    RaftLogic(int myid, int clusterSize) {
        this.myid = myid;
        this.clusterSize = clusterSize;
        this.electionTimer = myid;
        becameFollower();
    }

    public boolean isLeader() {
        return role == LEADER;
    }

    enum Role {
        LEADER, FOLLOWER, CANDIDATE
    }

    volatile Role role = FOLLOWER;

    // -- Figure 2
    // Persistent state on all servers
    int currentTerm = 0;
    Integer votedFor = null;

    Set<Integer> votes = new HashSet<>();
    int electionTimer;

    RaftLog log = new RaftLog();

    // Volatile state on all servers
    int commitIndex = 0;
    int lastApplied = 0;

    //  Volatile state on leader
    final Map<Integer, Integer> nextIndex = new HashMap<>();
    final Map<Integer, Integer> matchIndex = new HashMap<>();

    // eventually invoke via election
    void becomeLeader() {
        System.out.printf("Node %d: became LEADER\n", myid);
        role = LEADER;
        votedFor = null;
        resetTimer();
        votes.clear();
        for (int node = 1; node <= clusterSize; ++node) {
            nextIndex.put(node, log.highest_index + 1);
            matchIndex.put(node, 0);
        }
    }

    void becameFollower() {
        System.out.printf("Node %d: became FOLLOWER\n", myid);
        role = FOLLOWER;
        votedFor = null;
        resetTimer();
        votes.clear();
        matchIndex.clear();
        nextIndex.clear();
    }

    void becomeCandidate(Queue<RaftMessage> response) {
        System.out.printf("Node %d: became CANDIDATE\n", myid);
        role = CANDIDATE;
        ++currentTerm;
        votedFor = myid;
        votes.clear();
        votes.add(myid);
        resetTimer();
        sendRequestVote(response);
    }

    private void sendRequestVote(Queue<RaftMessage> response) {
        for (int node = 1; node <= clusterSize; ++node) {
            if (node != myid) response.add(new RaftMessage(myid, node,
                    new RequestVote(currentTerm, myid, log.highest_index, log.getLast().term())));
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
        matchIndex.put(myid, log.highest_index); // here or in receiveAppendEntriesResponse?
    }


    private void heartBeat(Queue<RaftMessage> response) {
        switch (role) {
            case LEADER -> {
                sendAppendEntriesToAll(response);
            }
            case CANDIDATE -> {
                ++electionTimer;
                if (electionTimer >= ELECTION_TIMER) {
                    becameFollower();
                } else
                    sendRequestVote(response);
            }
            case FOLLOWER -> {
                ++electionTimer;
                if (electionTimer >= ELECTION_TIMER) {
                    becomeCandidate(response);
                }
            }

        }
    }

    private void resetTimer() {
        electionTimer = new Random().nextInt(0, ELECTION_TIMER / 2);
    }

    private void sendAppendEntriesToAll(Queue<RaftMessage> response) {
        for (int node = 1; node <= clusterSize; ++node)
            if (node != myid) sendAppendEntries(node, response);
    }


    private void sendAppendEntries(int follower, Queue<RaftMessage> response) {
        assert role == LEADER;
        int prevLogIndex = nextIndex.get(follower) - 1;
        int lastEntry = min(log.highest_index, nextIndex.get(follower));
        // This list of entries might be huge
        // In practice it's better to send entries in chunks
        var entries = log.getEntries(prevLogIndex, log.highest_index);
        int prevLogTerm = entries.getFirst().term();
        entries = new ArrayList<>(entries.subList(1, entries.size()));

        var appendEntries = new AppendEntries(
                currentTerm, prevLogIndex, prevLogTerm, entries, min(commitIndex, lastEntry)); // mcommitIndex   |-> Min({commitIndex[i], lastEntry}),

        response.add(new RaftMessage(myid, follower, appendEntries));
    }


    private void receiveAppendEntries(int leader, AppendEntries msg, Queue<RaftMessage> response) {
        if (role == CANDIDATE) {
            if (currentTerm <= msg.term())
                becameFollower();
            else {
                response.add(new RaftMessage(myid, leader, new AppendEntriesResponse(currentTerm, false, -1)));
                return;
            }
        }
        assert role == FOLLOWER;
        resetTimer();
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
            commitIndex = min(msg.leaderCommit(), log.highest_index);

        response.add(new RaftMessage(myid, leader,
                new AppendEntriesResponse(currentTerm, true, msg.prevLogIndex() + msg.entries().size())));
    }

    private void receiveAppendEntriesResponse(int follower, AppendEntriesResponse msg) {
        System.err.printf("receiveAppendEntriesResponse(%d, %s)\n", follower, msg);
        if (msg.term() < currentTerm || role != LEADER) return;

        if (msg.success()) {
            nextIndex.put(follower, msg.matchIndex() + 1);
            matchIndex.put(follower, msg.matchIndex());
            matchIndex.put(myid, log.highest_index);
            advanceCommitIndex();
        } else {
            nextIndex.put(follower, max(nextIndex.get(follower) - 1, 1)); // maybe add defencive programming
            if (currentTerm < msg.term())
                becameFollower();
        }
    }

    private void advanceCommitIndex() {
        assert role == LEADER;
        // If there exists an N such that N > commitIndex, a majority
        // of matchIndex[i] ≥ N, and log[N].term == currentTerm:
        // set commitIndex = N (§5.3, §5.4).
        if (log.getLast().term() != currentTerm)
            return;

        var values = new ArrayList<>(this.matchIndex.values());
        sort(values);

        int newCommitIndex = values.get(clusterSize / 2);

        commitIndex = Math.max(commitIndex, newCommitIndex);
    }


    private void receiveRequestVote(int candidate, RequestVote msg, Queue<RaftMessage> response) {
        var logOk = msg.lastLogTerm() > log.getLast().term()
                    || (msg.lastLogTerm() == log.getLast().term() && msg.lastLogIndex() >= log.highest_index);

        var grant = msg.term() > currentTerm && logOk && (votedFor == null || votedFor == candidate);

        if (grant) {
            becameFollower();
            votedFor = candidate;
            currentTerm = msg.term();
        }
        response.add(new RaftMessage(myid, candidate, new RaftMessage.RequestVoteResponse(currentTerm, grant)));
    }

    private void receiveRequestVoteResponse(int from, RequestVoteResponse msg, Queue<RaftMessage> response) {
        // CANDIDATE sends RequestVote
        // When RequestVoteResponse come, CANDIDATE can have the majority and became a LEADER.
        // If CANDIDATE became a LEADER, it just skips additional votes.
        // If election time is over and CANDIDATE didn't get enough votes, it becomes FOLLOWER.
        // If CANDIDATE became a FOLLOWER, it just skips late votes.
        if (role != CANDIDATE) return;

        if (currentTerm == msg.term() && msg.voteGranted()) {
            votes.add(from);
            if (votes.size() >= 1 + clusterSize / 2) {
                becomeLeader();
                sendAppendEntriesToAll(response);
            }
        }

    }
}
