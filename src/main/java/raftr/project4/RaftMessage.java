package raftr.project4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RaftMessage {

    record AppendEntries(
            int term,
            int prevLogIndex,
            int prevLogTerm,
            List<String> entries,
            int leaderCommit) {}

    record AppendEntriesResponse(int term, boolean success) {}

    record RequestVote() {}

    record RequestVoteResponse() {}

    static Map<String, String> parseFields(String message) {
        Map<String, String> fields = new HashMap<>();
        int e = message.indexOf('[');

        fields.put("messageType", message.substring(0, e));
        String body = message.substring(e + 1, message.length() - 1);
        for (String field : body.split(", ")) {
            var kv = field.split("=", 2);
            fields.put(kv[0], kv[1]); // fails on lists
        }
        return fields;
    }

    public static void main(String[] args) {
        AppendEntries x = new AppendEntries(1, 2, 3,  List.of("5", "6"), 7);
        System.out.println(x);
        System.out.println(parseFields(x.toString()));
    }
}
