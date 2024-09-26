package raftr.project6;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raftr.project6.RaftLog.LogEntry;
import raftr.project6.RaftMessage.AppendEntries;
import raftr.project6.RaftMessage.AppendEntriesResponse;
import raftr.project6.RaftMessage.SendHeartBeat;
import raftr.project6.RaftMessage.SubmitCommand;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RaftLogicTest {

    private RaftLogic leader;
    private RaftLogic follower;

    @BeforeEach void setupClusterOfTwo() {
        leader = new RaftLogic(1, 2);
        leader.becomeLeader();
        follower = new RaftLogic(2, 2);
    }

    @Test void submitNewCommand_leader_sends_all_entries() {
        assert leader.receiveMessage(new RaftMessage(1, 1, new SubmitCommand("cmd"))).isEmpty();
        var response1 = leader.receiveMessage(new RaftMessage(1, 1, new SendHeartBeat()));
        assert response1.size() == 1;
        var appendEntries1 = response1.poll().asAppendEntries();
        assertThat(appendEntries1).isEqualTo(new AppendEntries(0, 0, -1, List.of(new LogEntry(0, "cmd")), 0));

        assert leader.receiveMessage(new RaftMessage(1, 1, new SubmitCommand("xxx"))).isEmpty();
        var response2 = leader.receiveMessage(new RaftMessage(1, 1, new SendHeartBeat()));
        assert response2.size() == 1;
        var appendEntries2 = response2.poll().asAppendEntries();
        assertThat(appendEntries2).isEqualTo(
                new AppendEntries(0, 0, -1, List.of(new LogEntry(0, "cmd"), new LogEntry(0, "xxx")), 0));

        assert leader.receiveMessage(new RaftMessage(1, 1, new SubmitCommand("zzz"))).isEmpty();
        var response3 = leader.receiveMessage(new RaftMessage(1, 1, new SendHeartBeat()));
        assert response3.size() == 1;
        var appendEntries3 = response3.poll().asAppendEntries();
        assertThat(appendEntries3).isEqualTo(new AppendEntries(0, 0, -1, List.of(new LogEntry(0, "cmd"), new LogEntry(0, "xxx"), new LogEntry(0, "zzz")), 0));
    }

    @Test void submitNewCommand_follower_replicates() {
        assert leader.receiveMessage(new RaftMessage(1, 1, new SubmitCommand("cmd"))).isEmpty();
        var response1 = leader.receiveMessage(new RaftMessage(1, 1, new SendHeartBeat()));
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
        assert leader.receiveMessage(new RaftMessage(1, 1, new SubmitCommand("cmd"))).isEmpty();
        // leader sent heartbeat that includes 'cmd'
        var response = leader.receiveMessage(new RaftMessage(1, 1, new SendHeartBeat()));
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


        assert leader.receiveMessage(new RaftMessage(1, 1, new SubmitCommand("xxx"))).isEmpty();
        response = leader.receiveMessage(new RaftMessage(1, 1, new SendHeartBeat()));
        assert response.size() == 1;
        var msg2 = response.poll();
        assertThat(msg2.asAppendEntries()).isEqualTo(new AppendEntries(0, 1, 0, List.of(new LogEntry(0, "xxx")), 1));
        response = follower.receiveMessage(msg2);
        leader.receiveMessage(response.poll());
        assert leader.commitIndex == 2;
    }
}