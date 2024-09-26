package raftr.project8;

import org.junit.jupiter.api.Test;

import static java.lang.System.err;
import static org.assertj.core.api.Assertions.assertThat;
import static raftr.project8.RaftTestUtils.ClusterBuidlers.*;
import static raftr.project8.RaftTestUtils.MessageBuilders.*;
import static raftr.project8.RaftTestUtils.assertAtMostOneLeader;
import static raftr.project8.RaftTestUtils.assertLogIsReplicated;

class RaftLogicClusterTest {

    @Test void test_cluster() {
        var cluster = createFakeCluster(2);
        cluster[1].becomeLeader();

        // Add a new application command
        deliverAllMessages(cluster, queue(submit(1, "set x 42"), heartbeat(1)));

        // At conclusion, logs should be equal
        assertThat(cluster[1].log.entries).isEqualTo(cluster[2].log.entries);
        assertThat(cluster[1].nextIndex.get(2)).isEqualTo(2);
    }

    @Test void test_figure_7() {
        // Testing concept: The Raft paper shows different configurations of
        // machines that might occur when a new leader comes to power in Figure 7.
        // The primary purpose of a leader is to replicate its log -- making all servers
        // look like itself. So, one idea is to create a Raft cluster based on
        // figure 7. Have the leader add a new command and start issuing heartbeats.
        // After some amount of time, all of the servers should look the same.
        var cluster = createFigure7();
        cluster[1].becomeLeader();
        deliverMessages(cluster, queue(submit(1, "commnd")));

        // Try replicating the log by sending out repeated updates.
        // Picked 10 updates arbitrarily (seemed like it would be enough).
        for (int i = 0; i < 10; i++) {
            deliverAllMessages(cluster, queue(heartbeat(1)));
        }

        // All servers should be exactly the same as the leader now
        assertLogIsReplicated(cluster);
    }

    @Test void test_figure_7_clock() {
        int repeat = 1000;
        while (repeat-- > 0) {
            var cluster = createFigure7();
            cluster[1].becomeLeader();
            deliverMessages(cluster, queue(submit(1, "commnd")));

            // Run for awhile under clock ticks (heartbits in my case) only
            runForaWhile(cluster, /*heartbits=*/ 10);

            // All servers should be exactly the same as the leader now
            assertLogIsReplicated(cluster);
        }
    }


    @Test void test_figure_7_election() {
        int repeat = 1000;
        while (repeat-- > 0) {
            var cluster = createFigure7();


            // Run for awhile under clock ticks (heartbits in my case) only
            // Should be a leader. Logs the same.
            runForaWhile(cluster, /*heartbits=*/ 300);

            // All servers should be exactly the same as the leader now
            err.printf("Assert repeat: \n", repeat);
            assertAtMostOneLeader(cluster);
        }
    }
}
