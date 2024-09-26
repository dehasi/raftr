package raftr.project5;

import raftr.project5.RaftLog.LogEntry;

import java.io.Serializable;
import java.util.List;

record RaftMessage(int src, int dst, Object msg) implements Serializable {

    boolean isAppendEntries() {
        return msg instanceof AppendEntries;
    }

    AppendEntries asAppendEntries() {
        assert isAppendEntries();
        return (AppendEntries) msg;
    }

    boolean isAppendEntriesResponse() {
        return msg instanceof AppendEntriesResponse;
    }

    AppendEntriesResponse asAppendEntriesResponse() {
        assert isAppendEntriesResponse();
        return (AppendEntriesResponse) msg;
    }

    boolean isRequestVote() {
        return msg instanceof RequestVote;
    }

    RequestVote asRequestVote() {
        assert isRequestVote();
        return (RequestVote) msg;
    }

    boolean isRequestVoteResponse() {
        return msg instanceof RequestVoteResponse;
    }

    RequestVoteResponse asRequestVoteResponse() {
        assert isRequestVoteResponse();
        return (RequestVoteResponse) msg;
    }

    record AppendEntries(
            int term,
            int prevLogIndex,
            int prevLogTerm,
            List<LogEntry> entries,
            int leaderCommit) implements Serializable {}

    record AppendEntriesResponse(int term, boolean success, int matchIndex) implements Serializable {}

    record RequestVote() implements Serializable {}

    record RequestVoteResponse() implements Serializable {}
}
