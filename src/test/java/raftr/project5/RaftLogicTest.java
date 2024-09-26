package raftr.project5;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raftr.project5.RaftLog.LogEntry;
import raftr.project5.RaftMessage.AppendEntries;
import raftr.project5.RaftMessage.AppendEntriesResponse;
import raftr.project5.RaftNet.RaftNetApp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

class RaftLogicTest {

    RaftNetApp raftnet1;
    RaftLogic logic1;
    RaftNetApp raftnet2;
    RaftLogic logic2;

    @BeforeEach void setupClusterOfTwo() {
        Map<Integer, Queue<RaftMessage>> network = new HashMap<>();
        raftnet1 = new RaftNetStub(1, network);
        logic1 = new RaftLogic(1, List.of(1, 2), raftnet1);
        logic1.becomeLeader();

        raftnet2 = new RaftNetStub(2, network);
        logic2 = new RaftLogic(2, List.of(1, 2), raftnet1);
    }

    @Test void submitNewCommand_leader_sends_all_entries() {
        logic1.submitNewCommand("cmd");
        logic1.sendAppendEntries(2);
        var appendEntries1 = raftnet2.receive().asAppendEntries();
        assertThat(appendEntries1).isEqualTo(new AppendEntries(0, 0, -1, List.of(new LogEntry(0, "cmd")), 0));

        logic1.submitNewCommand("xxx");
        logic1.sendAppendEntries(2);
        var appendEntries2 = raftnet2.receive().asAppendEntries();
        assertThat(appendEntries2).isEqualTo(
                new AppendEntries(0, 0, -1, List.of(new LogEntry(0, "cmd"), new LogEntry(0, "xxx")), 0));

        logic1.submitNewCommand("zzz");
        logic1.sendAppendEntries(2);
        var appendEntries3 = raftnet2.receive().asAppendEntries();
        assertThat(appendEntries3).isEqualTo(new AppendEntries(0, 0, -1, List.of(new LogEntry(0, "cmd"), new LogEntry(0, "xxx"), new LogEntry(0, "zzz")), 0));
    }

    @Test void submitNewCommand_follower_replicates() {
        logic1.submitNewCommand("cmd");
        logic1.sendAppendEntries(2);
        var msg = raftnet2.receive();
        logic2.receiveAppendEntries(msg.src(), msg.asAppendEntries());

        var response = raftnet1.receive();
        logic1.receiveAppendEntriesResponse(response.src(), response.asAppendEntriesResponse());
    }


    @Test void receiveAppendEntries_leader_updates_lastIndex() {
        logic1.submitNewCommand("cmd");
        logic1.sendAppendEntries(2);
        var msg = raftnet2.receive();
        assertThat(msg.asAppendEntries()).isEqualTo(new AppendEntries(0, 0, -1, List.of(new LogEntry(0, "cmd")), 0));
        logic2.receiveAppendEntries(msg.src(), msg.asAppendEntries());

        var response = raftnet1.receive();
        logic1.receiveAppendEntriesResponse(response.src(), response.asAppendEntriesResponse());

        logic1.submitNewCommand("xxx");
        logic1.sendAppendEntries(2);
        var msg2 = raftnet2.receive();
        assertThat(msg2.asAppendEntries()).isEqualTo(new AppendEntries(0, 1, 0, List.of(new LogEntry(0, "xxx")), 1));
        logic2.receiveAppendEntries(msg.src(), msg2.asAppendEntries());
    }

    @Test void receiveAppendEntries_heartbeat() {
        // sends empty list as heartbeat
        logic1.sendAppendEntries(2);
        var hb1 = raftnet2.receive();
        final int term0 = 0;
        assertThat(hb1.asAppendEntries()).isEqualTo(new AppendEntries(term0, 0, -1, List.of(), 0));
        logic2.receiveAppendEntries(hb1.src(), hb1.asAppendEntries());
        var hbr1 = raftnet1.receive();
        assertThat(hbr1.asAppendEntriesResponse()).isEqualTo(new AppendEntriesResponse(term0, true, 0));

        // append entry
        logic1.submitNewCommand("cmd");
        logic1.sendAppendEntries(2);
        var ae1 = raftnet2.receive();
        assertThat(ae1.asAppendEntries()).isEqualTo(new AppendEntries(term0, 0, -1, List.of(new LogEntry(0, "cmd")), 0));
        logic2.receiveAppendEntries(ae1.src(), ae1.asAppendEntries());
        var aer1 = raftnet1.receive();
        assertThat(aer1.asAppendEntriesResponse()).isEqualTo(new AppendEntriesResponse(term0, true, 1));
        logic1.receiveAppendEntriesResponse(aer1.src(), aer1.asAppendEntriesResponse());

        // append entry
        logic1.submitNewCommand("xxx");
        logic1.sendAppendEntries(2);
        var ae2 = raftnet2.receive();
        assertThat(ae2.asAppendEntries()).isEqualTo(new AppendEntries(term0, 1, 0, List.of(new LogEntry(term0, "xxx")), 1));
        logic2.receiveAppendEntries(ae2.src(), ae2.asAppendEntries());
        var aer2 = raftnet1.receive();
        assertThat(aer2.asAppendEntriesResponse()).isEqualTo(new AppendEntriesResponse(term0, true, 2));
        logic1.receiveAppendEntriesResponse(aer2.src(), aer2.asAppendEntriesResponse());
    }

    static class RaftNetStub extends RaftNetApp {

        private final Map<Integer, Queue<RaftMessage>> messageQueue;

        public RaftNetStub(int nodenum, Map<Integer, Queue<RaftMessage>> network) {
            super(nodenum, null);
            this.messageQueue = network;
            network.put(nodenum, new ArrayBlockingQueue<>(5));
        }

        @Override void send(int destination, RaftMessage msg) {
            messageQueue.putIfAbsent(destination, new ArrayBlockingQueue<>(5));
            messageQueue.get(destination).add(msg);
        }

        @Override RaftMessage receive() {
            return messageQueue.get(super.nodenum).poll();
        }
    }
}