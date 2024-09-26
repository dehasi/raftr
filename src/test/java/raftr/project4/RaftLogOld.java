package raftr.project4;

import raftr.project4.RaftMessage.AppendEntries;
import raftr.project4.RaftMessage.AppendEntriesResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.max;

class RaftLogOld {

    record LogEntry(int term, String message) {}

    List<LogEntry> log = new ArrayList<>();
    int currentTerm = 1;
    int commitIndex = 0;
    int highest_index = 0;
    Map<Integer, LogEntry> entries = new HashMap<>();


    public RaftLogOld() {
        this(List.of());
    }

    public RaftLogOld(List<LogEntry> initial_entries) {
        for (int n = 1; n <= initial_entries.size(); ++n) {
            this.entries.put(n, initial_entries.get(n - 1));
            this.highest_index = n;
        }

    }

    // Append a new command at the end of the.  Performed by leaders. Always works.
    void append_new_command(int term, String command) {
        highest_index += 1;
        entries.put(highest_index, new LogEntry(term, command));
    }

    // Append entries to the log. Performed by followers. Might fail (if holes, etc.)
    boolean append_entries(int prev_index, int prev_term, List<LogEntry> entries) {
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

    List<LogEntry> get_entries(int from, int to) {
        List<LogEntry> result = new ArrayList<>();
        for (int i = from; i <= to; ++i)
            result.add(this.entries.get(i));
        return result;
    }


    AppendEntriesResponse appendEntries(AppendEntries entries) {
        // (1)
        if (entries.term() < currentTerm)
            return new AppendEntriesResponse(currentTerm, false);

        // (2)
        if (log.isEmpty()) {
            if (entries.prevLogIndex() != 0)
                return new AppendEntriesResponse(currentTerm, false);
        } else {
            if (log.size() < entries.prevLogIndex() || log.get(entries.prevLogIndex() - 1).term != entries.prevLogTerm()) {
                return new AppendEntriesResponse(currentTerm, false);
            }
        }

        // (3)
        // If an existing entry conflicts with a new one
        // same index but different terms
        if (!log.isEmpty() && log.get(entries.prevLogIndex() - 1).term != entries.term()) {
            // delete the existing entry and all that follow it
            log = new ArrayList<>(log.subList(0, entries.prevLogIndex()));
            // log.subList(entries.prevLogIndex()-1, log.size()).clear();
        }

        if (log.size() == entries.prevLogIndex()) {
            // (4)
            entries.entries().stream()
                    .map(msg -> new LogEntry(entries.term(), msg))
                    .forEach(log::add);
        } // else we update log in the middle = skip

        // (5)
        if (entries.leaderCommit() > commitIndex)
            commitIndex = min(entries.leaderCommit(), log.size());
        // Once a follower learns that a log entry is committed, it applies the entry to its local state machine (in log order).

        return new AppendEntriesResponse(currentTerm, true);
    }
}
