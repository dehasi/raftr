package raftr.project3;

import static raftr.project3.RaftLogic.Role.FOLLOWER;

class RaftLogic {
    int currentTerm =1;
    Role role = FOLLOWER;

    enum Role {
        LEADER, FOLLOWER, CANDIDATE
    }
}
