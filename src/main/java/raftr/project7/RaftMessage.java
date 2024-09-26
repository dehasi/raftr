package raftr.project7;

import raftr.project7.RaftLog.LogEntry;

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


    boolean isSubmitCommand() {
        return msg instanceof SubmitCommand;
    }

    SubmitCommand asSubmitCommand() {
        assert isSubmitCommand();
        return (SubmitCommand) msg;
    }

    boolean isSendHeartBeat() {
        return msg instanceof SendHeartBeat;
    }

    SendHeartBeat asSendHeartBeat() {
        assert isSendHeartBeat();
        return (SendHeartBeat) msg;
    }

    record AppendEntries(
            int term,
            int prevLogIndex,
            int prevLogTerm,
            List<LogEntry> entries,
            int leaderCommit) implements Serializable {}

    record AppendEntriesResponse(int term, boolean success, int matchIndex) implements Serializable {}

    record RequestVote(int term, int candidateId, int lastLogIndex, int lastLogTerm) implements Serializable {}

    record RequestVoteResponse(int term, boolean voteGranted) implements Serializable {}

    record SubmitCommand(String cmd) implements Serializable {}

    record SendHeartBeat() implements Serializable {}
}
