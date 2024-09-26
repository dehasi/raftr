package raftr.project8;

import raftr.project8.RaftLog;
import raftr.project8.RaftLogic;
import raftr.project8.RaftMessage;
import raftr.project8.RaftMessage.SubmitCommand;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class RaftTestUtils {

    static class MessageBuilders {
        static RaftMessage heartbeat() {
            return heartbeat(1);
        }

        static RaftMessage heartbeat(int id) {
            return new RaftMessage(id, id, new RaftMessage.SendHeartBeat());
        }

        static RaftMessage submit(int id, String cmd) {
            return msg(id, id, submit(cmd));
        }

        static SubmitCommand submit(String cmd) {
            return new SubmitCommand(cmd);
        }

        static RaftMessage msg(int src, int dst, SubmitCommand cmd) {
            return new RaftMessage(src, dst, cmd);
        }

        static Queue<RaftMessage> queue(RaftMessage... messages) {
            return new LinkedList<>(Arrays.asList(messages));
        }
    }

    static class ClusterBuidlers {
        static RaftLogic[]
        createFakeCluster(int size) {
            var cluster = new RaftLogic[size + 1]; // eliminate index[0]
            for (int id = 1; id <= size; id++)
                cluster[id] = new RaftLogic(id, size);
            return cluster;
        }

        static Queue<RaftMessage>
        deliverMessages(RaftLogic[] cluster, Queue<RaftMessage> messages) {
            Queue<RaftMessage> outgoing = new LinkedList<>();

            while (!messages.isEmpty()) {
                var msg = messages.poll();
                var response = cluster[msg.dst()].receiveMessage(msg);
                outgoing.addAll(response);

                assertAtMostOneLeader(cluster);
            }
            return outgoing;
        }

        static void
        deliverAllMessages(RaftLogic[] cluster, Queue<RaftMessage> messages) {
            while (!messages.isEmpty()) {
                messages.addAll(deliverMessages(cluster, messages));
            }
        }

        static void
        runForaWhile(RaftLogic[] cluster, int ticks) {
            while (ticks-- > 0) {
                Queue<RaftMessage> outgoing = new LinkedList<>();
                for (int id = 1; id < cluster.length; id++)
                    outgoing.add(MessageBuilders.heartbeat(id));

                deliverAllMessages(cluster, outgoing);
            }
        }

        private static RaftLog makeLog(int... terms) {
            var log = new RaftLog();
            for (var t : terms)
                log.appendNewCommand(t, "");
            return log;
        }

        static RaftLogic[]
        createFigure7() {
            RaftLogic[] cluster = createFakeCluster(7);
            cluster[1].log = makeLog(1, 1, 1, 4, 4, 5, 5, 6, 6, 6);
            cluster[2].log = makeLog(1, 1, 1, 4, 4, 5, 5, 6, 6);
            cluster[3].log = makeLog(1, 1, 1, 4);
            cluster[4].log = makeLog(1, 1, 1, 4, 4, 5, 5, 6, 6, 6, 6);
            cluster[5].log = makeLog(1, 1, 1, 4, 4, 5, 5, 6, 6, 6, 7, 7);
            cluster[6].log = makeLog(1, 1, 1, 4, 4, 4, 4);
            cluster[7].log = makeLog(1, 1, 1, 2, 2, 2, 3, 3, 3, 3, 3);
            return cluster;
        }
    }

    // Election safety.  At most one leader can be elected in a given term.
     static void assertAtMostOneLeader(RaftLogic[] cluster) {
        int leaderCount = 0;
        for (int id = 1; id < cluster.length; id++)
            if (cluster[id].isLeader()) ++leaderCount;
        assert leaderCount <= 1;
    }

    // All servers should be exactly the same as the leader now
    static void assertLogIsReplicated(RaftLogic[] cluster) {
        for (int id = 1; id < cluster.length; id++) {
            assertThat(cluster[1].log.entries).as("node: %d", id).isEqualTo(cluster[id].log.entries);
        }
    }
}
