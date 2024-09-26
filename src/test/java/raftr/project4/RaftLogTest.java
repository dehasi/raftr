package raftr.project4;

import org.junit.jupiter.api.Test;
import raftr.project4.RaftLog.LogEntry;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaftLogTest {

    RaftLog log = new RaftLog();

    @Test void append_entries() {
        // Appending a new entry to an empty log needs to work. (Does prev_term matter?)
        assertTrue(log.appendEntries(0, 0, List.of(new LogEntry(1, "x"))));
        assertTrue(log.appendEntries(0, 0, List.of(new LogEntry(1, "x"))));    // Repeated ops okay
        assertTrue(log.highest_index == 1);

        // Not allowed to holes or gaps
        assertFalse(log.appendEntries(10, 1, List.of(new LogEntry(1, "y"))));

        // Term must match up
        assertTrue(log.appendEntries(1, 1, List.of(new LogEntry(1, "y"))));
        assertTrue(log.highest_index == 2);

        // Repeated ops
        assertTrue(log.appendEntries(1, 1, List.of(new LogEntry(1, "y"))));
        assertTrue(log.highest_index == 2);

        // Fail.  prev_term doesn't match up
        assertFalse(log.appendEntries(2, 2, List.of(new LogEntry(2, "z"))));

        // Appending empty entries must work as long as the prev_index, prev_term values match up
        assertTrue(log.appendEntries(2, 1, List.of()));
        assertFalse(log.appendEntries(2, 2, List.of()));

        //  Test deletion of existing entries
        log = new RaftLog(List.of(
                new LogEntry(1, "x"),
                new LogEntry(1, "y"),
                new LogEntry(1, "z"),
                new LogEntry(2, "a"),
                new LogEntry(2, "b"),
                new LogEntry(2, "c")
        ));
        assertTrue(log.highest_index == 6);

        // Should not delete
        assertTrue(log.appendEntries(2, 1, List.of(new LogEntry(1, "z"))));
        assertTrue(log.highest_index == 6);
        assertThat(log.getEntries(1, 6)).containsExactly(
                new LogEntry(1, "x"), new LogEntry(1, "y"), new LogEntry(1, "z"),
                new LogEntry(2, "a"), new LogEntry(2, "b"), new LogEntry(2, "c"));

        // This will delete.  Term of term entry mismatches existing entry
        assertTrue(log.appendEntries(2, 1, List.of(new LogEntry(2, "k"))));
        assertTrue(log.highest_index == 3);
        assertThat(log.getEntries(1, 3)).containsExactly(
                new LogEntry(1, "x"), new LogEntry(1, "y"), new LogEntry(2, "k"));
    }

    RaftLog make_log(List<Integer> terms) {
        var log = new RaftLog();
        for (var t : terms)
            log.appendNewCommand(t, "");
        return log;
    }

    @Test void test_figure_7() {
        //  Figure 7 shows some possible log configurations when a leader comes to power.
        // We'll try making some of these logs and verify our understanding of append_entries

        // (a)
        log = make_log(List.of(1, 1, 1, 4, 4, 5, 5, 6, 6));
        assertFalse(log.appendEntries(10, 6, List.of(new LogEntry(8, ""))));

        // (c)
        log = make_log(List.of(1, 1, 1, 4, 4, 5, 5, 6, 6, 6, 6));
        assertTrue(log.appendEntries(10, 6, List.of(new LogEntry(8, ""))));
        assertThat(log.getEntries(1, 11).stream().map(LogEntry::term).toList())
                .containsExactly(1, 1, 1, 4, 4, 5, 5, 6, 6, 6, 8);
        assertTrue(log.highest_index == 11);

        // (d)
        log = make_log(List.of(1, 1, 1, 4, 4, 5, 5, 6, 6, 6, 7, 7));
        assertTrue(log.appendEntries(10, 6, List.of(new LogEntry(8, ""))));
        assertThat(log.getEntries(1, 11).stream().map(LogEntry::term).toList())
                .containsExactly(1, 1, 1, 4, 4, 5, 5, 6, 6, 6, 8);
        assertTrue(log.highest_index == 11);

        // (f)
        log = make_log(List.of(1, 1, 1, 2, 2, 2, 3, 3, 3, 3, 3));
        assertFalse(log.appendEntries(10, 6, List.of(new LogEntry(8, ""))));
    }
}
