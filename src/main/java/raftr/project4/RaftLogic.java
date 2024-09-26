package raftr.project4;

import static raftr.project4.RaftLogic.Role.FOLLOWER;

class RaftLogic {
    int currentTerm =1;
    Role role = FOLLOWER;

    enum Role {
        LEADER, FOLLOWER, CANDIDATE
    }
}
