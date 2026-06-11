package network.server;

import network.protocol.GameStateDto;

/**
 * 联机获胜后的全票再来一局投票跟踪。
 * <p>
 * 所有活跃玩家投赞成时，{@link GameSession} 重新开始一局。
 * 一人拒绝则对所有人关闭投票。
 */
final class SessionRematchManager {
    private final Boolean[] rematchVotes;
    private boolean rematchOpen;
    private boolean rematchDeclined;

    SessionRematchManager(int maxPlayers) {
        rematchVotes = new Boolean[maxPlayers];
    }

    /** 获胜后玩家仍可投票时为 true。 */
    boolean isRematchOpen() {
        return rematchOpen;
    }

    /** 任一玩家拒绝再来一局后为 true。 */
    boolean isRematchDeclined() {
        return rematchDeclined;
    }

    /** 该座位尚未投票时返回 null。 */
    Boolean voteForSeat(int seat) {
        if (seat < 0 || seat >= rematchVotes.length) {
            return null;
        }
        return rematchVotes[seat];
    }

    /** 新一局开始时重置再来一局状态。 */
    void clear() {
        rematchOpen = false;
        rematchDeclined = false;
        java.util.Arrays.fill(rematchVotes, null);
    }

    /** 有玩家获胜后开启投票。 */
    void open() {
        rematchOpen = true;
        rematchDeclined = false;
        java.util.Arrays.fill(rematchVotes, null);
    }

    /** 将再来一局投票进度写入指定座位的 STATE 快照。 */
    void enrichState(GameStateDto state, int seat, int playerCount) {
        state.rematchOpen = rematchOpen;
        state.rematchRequired = playerCount;
        state.rematchYesCount = countYesVotes(playerCount);
        state.rematchDeclined = rematchDeclined;
        if (seat >= 0 && seat < rematchVotes.length) {
            state.myRematchVote = rematchVotes[seat];
        }
    }

    /**
     * 记录一名玩家的再来一局投票。
     * 所有活跃玩家投赞成时返回 RESTART。
     */
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

    /** 记录一次再来一局投票后的结果类别。 */
    enum VoteKind {
        ERROR,
        DECLINED,
        WAITING,
        RESTART
    }

    /** {@link #recordVote} 的结果；RESTART 包含新一局所需的玩家数。 */
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
