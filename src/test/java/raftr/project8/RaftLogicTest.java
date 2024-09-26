package raftr.project8;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raftr.project8.RaftLog.LogEntry;
import raftr.project8.RaftLogic;
import raftr.project8.RaftMessage;
import raftr.project8.RaftMessage.AppendEntries;
import raftr.project8.RaftMessage.AppendEntriesResponse;
import raftr.project8.RaftMessage.RequestVote;
import raftr.project8.RaftMessage.RequestVoteResponse;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static raftr.project8.RaftConfig.ELECTION_TIMER;
import static raftr.project8.RaftLogic.Role.CANDIDATE;
import static raftr.project8.RaftTestUtils.MessageBuilders.*;

class RaftLogicTest {

    private RaftLogic leader;
    private RaftLogic follower;

    @BeforeEach void setupClusterOfTwo() {
        leader = new RaftLogic(1, 2);
        leader.becomeLeader();
        follower = new RaftLogic(2, 2);
    }

    @Test void submitNewCommand_leader_sends_all_entries() {
        assert leader.receiveMessage(msg(1, 1, submit("cmd"))).isEmpty();
        var response1 = leader.receiveMessage(heartbeat());
        assert response1.size() == 1;
        var appendEntries1 = response1.poll().asAppendEntries();
        assertThat(appendEntries1).isEqualTo(new AppendEntries(0, 0, -1, List.of(new LogEntry(0, "cmd")), 0));

        assert leader.receiveMessage(msg(1, 1, submit("xxx"))).isEmpty();
        var response2 = leader.receiveMessage(heartbeat());
        assert response2.size() == 1;
        var appendEntries2 = response2.poll().asAppendEntries();
        assertThat(appendEntries2).isEqualTo(
                new AppendEntries(0, 0, -1, List.of(new LogEntry(0, "cmd"), new LogEntry(0, "xxx")), 0));

        assert leader.receiveMessage(msg(1, 1, submit("zzz"))).isEmpty();
        var response3 = leader.receiveMessage(heartbeat());
        assert response3.size() == 1;
        var appendEntries3 = response3.poll().asAppendEntries();
        assertThat(appendEntries3).isEqualTo(new AppendEntries(0, 0, -1, List.of(new LogEntry(0, "cmd"), new LogEntry(0, "xxx"), new LogEntry(0, "zzz")), 0));
    }

    @Test void submitNewCommand_follower_replicates() {
        assert leader.receiveMessage(msg(1, 1, submit("cmd"))).isEmpty();
        var response1 = leader.receiveMessage(heartbeat());
        assert response1.size() == 1;
        var toFollower1 = response1.poll();

        var response2 = follower.receiveMessage(toFollower1);
        assert response2.size() == 1;

        var toLeader = response2.poll();
        assert toLeader.asAppendEntriesResponse().success();
    }


    @Test void receiveAppendEntries_leader_updates_lastIndex() {
        final int term0 = 0;
        assert leader.commitIndex == 0;
        // leader received 'cmd'
        assert leader.receiveMessage(msg(1, 1, submit("cmd"))).isEmpty();
        // leader sent heartbeat that includes 'cmd'
        var response = leader.receiveMessage(heartbeat());
        assert response.size() == 1;
        assertThat(response.peek().asAppendEntries()).isEqualTo(new AppendEntries(term0, 0, -1, List.of(new LogEntry(0, "cmd")), 0));

        // follower received a heartbeat that includes 'cmd'
        var toFollower1 = response.poll();
        response = follower.receiveMessage(toFollower1);
        // follower responsed with match index 1
        assert response.size() == 1;
        assertThat(response.peek().asAppendEntriesResponse()).isEqualTo(new AppendEntriesResponse(term0, true, 1));

        // leader receives AppendEntriesResponse and updates commit index
        var toLeader = response.poll();
        leader.receiveMessage(toLeader);
        assert leader.commitIndex == 1;


        assert leader.receiveMessage(msg(1, 1, submit("xxx"))).isEmpty();
        response = leader.receiveMessage(heartbeat());
        assert response.size() == 1;
        var msg2 = response.poll();
        assertThat(msg2.asAppendEntries()).isEqualTo(new AppendEntries(0, 1, 0, List.of(new LogEntry(0, "xxx")), 1));
        response = follower.receiveMessage(msg2);
        leader.receiveMessage(response.poll());
        assert leader.commitIndex == 2;
    }

    @Test void election() {
        final int term0 = 0;
        final int term1 = 1;
        prefillLog();
        // Leader stopped sending AppendEntries

        Queue<RaftMessage>response =new LinkedList<>();
        for (int times = ELECTION_TIMER; times-- > 0 || follower.role != CANDIDATE; )
            response = follower.receiveMessage(heartbeat());
        assert follower.role == CANDIDATE;

        // follower sends RequestVote with new term
        assert response.size() == 1;
        assertThat(response.peek().asRequestVote()).isEqualTo(new RequestVote(term1, 2, 3, term0));
        // leader votes posotive and becomes follower
        response = leader.receiveMessage(response.poll());
        assert response.size() == 1;
        assertThat(response.peek().asRequestVoteResponse()).isEqualTo(new RequestVoteResponse(term1, true));
        assert leader.role == RaftLogic.Role.FOLLOWER;
        assert leader.votedFor == 2;
    }

    @Test void election2() {
        final int term0 = 0;
        final int term1 = 1;
        prefillLog();
        // leader is ahead but, stop send heartbeat
        assert leader.receiveMessage(msg(1, 1, submit("zzz"))).isEmpty();

        // follower becomes candidate
        Queue<RaftMessage> response = null;
        for (int times = ELECTION_TIMER; times-- > 0; ) {
            response = follower.receiveMessage(heartbeat());
        }
        assert follower.role == CANDIDATE;

        // follower sends RequestVote with new term
        assert response.size() == 1;
        assertThat(response.peek().asRequestVote()).isEqualTo(new RequestVote(term1, 2, 3, term0));
        // leader votes negative, because (msg.lastLogIndex() >= log.highest_index) is false
        response = leader.receiveMessage(response.poll());
        assert response.size() == 1;
        assertThat(response.peek().asRequestVoteResponse()).isEqualTo(new RequestVoteResponse(term0, false));
        assert leader.role == RaftLogic.Role.LEADER;
    }

    private void prefillLog() {
        final int term0 = 0;
        // fill log
        assert leader.receiveMessage(msg(1, 1, submit("cmd"))).isEmpty();
        assert leader.receiveMessage(msg(1, 1, submit("xxx"))).isEmpty();
        assert leader.receiveMessage(msg(1, 1, submit("yyy"))).isEmpty();

        // When heartbeat
        var response = leader.receiveMessage(heartbeat());
        // Then leader sends 3 entries in one message
        assert response.size() == 1;
        assertThat(response.peek().asAppendEntries().entries()).hasSize(3);

        response = follower.receiveMessage(response.poll());
        // follower responses with match index 3
        assert response.size() == 1;
        assertThat(response.peek().asAppendEntriesResponse()).isEqualTo(new AppendEntriesResponse(term0, true, 3));
        // candidate process response
        response = leader.receiveMessage(response.poll());
        assert response.isEmpty();
    }

}