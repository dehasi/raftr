package raftr.project6;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;

class RaftLog {

    record LogEntry(int term, String message) implements Serializable {}

    int highest_index = 0;
    Map<Integer, LogEntry> entries = new HashMap<>() {{
        put(0, new LogEntry(-1, "dummy"));
    }};


    RaftLog() {
        this(List.of());
    }

    RaftLog(List<LogEntry> initial_entries) {
        for (int n = 1; n <= initial_entries.size(); ++n) {
            this.entries.put(n, initial_entries.get(n - 1));
            this.highest_index = n;
        }

    }

    // Append a new command at the end of the.  Performed by leaders. Always works.
    void appendNewCommand(int term, String command) {
        highest_index += 1;
        entries.put(highest_index, new LogEntry(term, command));
    }

    // Append entries to the log. Performed by followers. Might fail (if holes, etc.)
    boolean appendEntries(int prev_index, int prev_term, List<LogEntry> entries) {
        // No holes are allowed in the log
        if (prev_index > 0 && !this.entries.containsKey(prev_index))
            return false;

        // Term must match up
        if (prev_index > 0 && this.entries.get(prev_index).term != prev_term)
            return false;

        //Put entries into the log
        int index = prev_index;
        for (var e : entries) {
            index += 1;
            // Extreme care is needed here. From Figure 2.
            //   "3. If an existing entry conflicts with a new one (same index, but different
            //       terms), delete the existing entry and all that follow it."
            if (this.entries.containsKey(index) && this.entries.get(index).term() != e.term()) {
                for (int i = index; i <= highest_index + 1; ++i)
                    this.entries.remove(i);
                highest_index = index;
            }
            this.entries.put(index, e);
        }

        highest_index = max(highest_index, index);
        return true;
    }

    LogEntry getEntry(int index) {
        assert index >= 0;
        return requireNonNull(this.entries.get(index), "No LogEntry at index " + index);
    }

    List<LogEntry> getEntries(int from, int to) {
        List<LogEntry> result = new ArrayList<>();
        for (int i = from; i <= to; ++i)
            result.add(this.entries.get(i));
        return result;
    }
}
