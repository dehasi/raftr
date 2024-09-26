package raftr.project4;

class RaftLogOldTest {

//    RaftLog log = new RaftLog();

//    @Test void appendEntries() {
//        // append empty
//        assertTrue(log.appendEntries(new AppendEntries(/*term=*/1,/*prevLogIndex=*/0, /*prevLogTerm=*/1, List.of("12"), 0)).success());
//
//        // no gaps
//        assertFalse(log.appendEntries(new AppendEntries(/*term=*/1, /*prevLogIndex=*/10, /*prevLogTerm=*/1, List.of("12"), 0)).success());
//
//        // Term must match up
//        assertTrue(log.appendEntries(new AppendEntries(/*term=*/1, /*prevLogIndex=*/1, /*prevLogTerm=*/1, List.of("14"), 0)).success());
//        assertFalse(log.appendEntries(new AppendEntries(/*term=*/2, /*prevLogIndex=*/2, /*prevLogTerm=*/2, List.of("14"), 0)).success());
//    }
//
//    @Test void happy_path() {
//        log.appendEntries(new AppendEntries(/*term=*/1,/*prevLogIndex=*/0, /*prevLogTerm=*/1, List.of("0"), 0));
//        log.appendEntries(new AppendEntries(/*term=*/1,/*prevLogIndex=*/1, /*prevLogTerm=*/1, List.of("1"), 0));
//        log.appendEntries(new AppendEntries(/*term=*/1,/*prevLogIndex=*/2, /*prevLogTerm=*/1, List.of("2"), 0));
//
//        assertThat(log.log).containsExactly(
//                new LogEntry(1, "0"),
//                new LogEntry(1, "1"),
//                new LogEntry(1, "2")
//        );
//
//        log.appendEntries(new AppendEntries(/*term=*/2,/*prevLogIndex=*/3, /*prevLogTerm=*/1, List.of("3"), 0));
//        log.appendEntries(new AppendEntries(/*term=*/2,/*prevLogIndex=*/4, /*prevLogTerm=*/2, List.of("4"), 0));
//
//        assertThat(log.log).containsExactly(
//                new LogEntry(1, "0"),
//                new LogEntry(1, "1"),
//                new LogEntry(1, "2"),
//                new LogEntry(2, "3"),
//                new LogEntry(2, "4")
//        );
//    }
//
//
//    @Test void idempotence() {
//        log.appendEntries(new AppendEntries(/*term=*/1,/*prevLogIndex=*/0, /*prevLogTerm=*/1, List.of("0"), 0));
//        log.appendEntries(new AppendEntries(/*term=*/1,/*prevLogIndex=*/1, /*prevLogTerm=*/1, List.of("1"), 0));
//        log.appendEntries(new AppendEntries(/*term=*/1,/*prevLogIndex=*/1, /*prevLogTerm=*/1, List.of("1"), 0));
//        log.appendEntries(new AppendEntries(/*term=*/1,/*prevLogIndex=*/1, /*prevLogTerm=*/1, List.of("1"), 0));
//        log.appendEntries(new AppendEntries(/*term=*/1,/*prevLogIndex=*/2, /*prevLogTerm=*/1, List.of("2"), 0));
//
//        assertThat(log.log).containsExactly(
//                new LogEntry(1, "0"),
//                new LogEntry(1, "1"),
//                new LogEntry(1, "2")
//        );
//    }
//
//    @Test void delete_conflicted() {
//        log.appendEntries(new AppendEntries(/*term=*/1,/*prevLogIndex=*/0, /*prevLogTerm=*/1, List.of("0"), 0));
//        log.appendEntries(new AppendEntries(/*term=*/1,/*prevLogIndex=*/1, /*prevLogTerm=*/1, List.of("1"), 1));
//        log.appendEntries(new AppendEntries(/*term=*/1,/*prevLogIndex=*/2, /*prevLogTerm=*/1, List.of("2"), 2));
//
//        assertThat(log.log).containsExactly(
//                new LogEntry(1, "0"),
//                new LogEntry(1, "1"),
//                new LogEntry(1, "2")
//        );
//
//        log.appendEntries(new AppendEntries(/*term=*/2,/*prevLogIndex=*/2, /*prevLogTerm=*/1, List.of("22"), 2));
//
//        assertThat(log.log).containsExactly(
//                new LogEntry(1, "0"),
//                new LogEntry(1, "1"),
//                new LogEntry(2, "22")
//        );
//    }
}
