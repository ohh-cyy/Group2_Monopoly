package network.server;

import network.protocol.GameStateDto;

/** Tracks rematch votes after an online game ends. */
final class SessionRematchManager {
    private final Boolean[] rematchVotes;
    private boolean rematchOpen;
    private boolean rematchDeclined;

    SessionRematchManager(int maxPlayers) {
        rematchVotes = new Boolean[maxPlayers];
    }

    boolean isRematchOpen() {
        return rematchOpen;
    }

    boolean isRematchDeclined() {
        return rematchDeclined;
    }

    Boolean voteForSeat(int seat) {
        if (seat < 0 || seat >= rematchVotes.length) {
            return null;
        }
        return rematchVotes[seat];
    }

    void clear() {
        rematchOpen = false;
        rematchDeclined = false;
        java.util.Arrays.fill(rematchVotes, null);
    }

    void open() {
        rematchOpen = true;
        rematchDeclined = false;
        java.util.Arrays.fill(rematchVotes, null);
    }

    void enrichState(GameStateDto state, int seat, int playerCount) {
        state.rematchOpen = rematchOpen;
        state.rematchRequired = playerCount;
        state.rematchYesCount = countYesVotes(playerCount);
        state.rematchDeclined = rematchDeclined;
        if (seat >= 0 && seat < rematchVotes.length) {
            state.myRematchVote = rematchVotes[seat];
        }
    }

    VoteOutcome recordVote(int seat, int playerCount, boolean accept, String playerName) {
        if (!rematchOpen) {
            return VoteOutcome.error("Rematch not available");
        }
        if (seat < 0 || seat >= playerCount) {
            return VoteOutcome.error("Invalid seat");
        }
        if (rematchVotes[seat] != null) {
            return VoteOutcome.error("Already voted");
        }
        rematchVotes[seat] = accept;
        if (!accept) {
            rematchOpen = false;
            rematchDeclined = true;
            return VoteOutcome.declined(playerName + " declined a rematch");
        }
        if (allYes(playerCount)) {
            return VoteOutcome.restart(playerCount, "All players voted yes — starting a new game");
        }
        return VoteOutcome.waiting(playerName + " wants a rematch");
    }

    private boolean allYes(int playerCount) {
        if (playerCount <= 0) {
            return false;
        }
        for (int i = 0; i < playerCount; i++) {
            if (!Boolean.TRUE.equals(rematchVotes[i])) {
                return false;
            }
        }
        return true;
    }

    private int countYesVotes(int playerCount) {
        int count = 0;
        for (int i = 0; i < playerCount; i++) {
            if (Boolean.TRUE.equals(rematchVotes[i])) {
                count++;
            }
        }
        return count;
    }

    enum VoteKind {
        ERROR,
        DECLINED,
        WAITING,
        RESTART
    }

    record VoteOutcome(VoteKind kind, String logLine, int restartPlayerCount) {
        static VoteOutcome error(String message) {
            return new VoteOutcome(VoteKind.ERROR, message, 0);
        }

        static VoteOutcome declined(String logLine) {
            return new VoteOutcome(VoteKind.DECLINED, logLine, 0);
        }

        static VoteOutcome waiting(String logLine) {
            return new VoteOutcome(VoteKind.WAITING, logLine, 0);
        }

        static VoteOutcome restart(int playerCount, String logLine) {
            return new VoteOutcome(VoteKind.RESTART, logLine, playerCount);
        }
    }
}
