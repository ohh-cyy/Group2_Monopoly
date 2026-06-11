package controller.network;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import network.protocol.GameStateDto;
import ui.render.TurnDeadlineClock;

/**
 * 在服务器同步间隔内保持联机回合截止标签刷新的客户端滴答器。
 * <p>
 * 使用 {@link network.protocol.GameStateDto#turnDeadlineEpochMillis}，
 * 并将标签刷新委托给 {@link Host}（通常为 {@link controller.NetworkGameController}）。
 */
public final class OnlineTurnTimerDisplay {
    /** 向本地玩家显示一次性警告时的剩余秒数。 */
    public static final int TURN_WARNING_SECONDS = 10;

    /**
     * 每秒读取状态并更新 UI 的回调。
     */
    public interface Host {
        /** 包含回合截止时间的最新服务器快照。 */
        GameStateDto state();

        /** 本地座位是否为当前行动玩家。 */
        boolean isMyTurn();

        /** 当前回合玩家的显示名称。 */
        String currentPlayerName();

        /** 重绘回合状态标签（运行期间每秒调用）。 */
        void refreshTimerLabel();

        /** 本地玩家回合达到警告阈值时每回合调用一次。 */
        void onTurnWarning(String playerName);
    }

    private final Host host;
    private Timeline timeline;
    private boolean warningShown;
    private int lastTurnIndex = -1;

    /**
     * @param host 状态读取与标签刷新的回调
     */
    public OnlineTurnTimerDisplay(Host host) {
        this.host = host;
    }

    /** 每次状态同步后调用，重置警告并确保滴答器运行。 */
    public void onStateUpdated() {
        resetWarningIfNewTurn();
        ensureRunning();
        host.refreshTimerLabel();
    }

    /** 停止每秒一次的 JavaFX 时间线（如游戏结束时）。 */
    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    /** 游戏暂停时暂停显示滴答器。 */
    public void pause() {
        if (timeline != null) {
            timeline.pause();
        }
    }

    /** 恢复已暂停的显示滴答器。 */
    public void resume() {
        if (timeline != null) {
            timeline.play();
        }
    }

    private void ensureRunning() {
        GameStateDto state = host.state();
        if (state == null || state.gameOver || (!state.gamePaused && state.turnDeadlineEpochMillis <= 0)) {
            stop();
            return;
        }
        if (timeline == null) {
            timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
        }
    }

    private void tick() {
        host.refreshTimerLabel();
        GameStateDto state = host.state();
        if (state == null || state.gameOver) {
            stop();
            return;
        }
        if (state.gamePaused) {
            return;
        }
        if (state.turnDeadlineEpochMillis <= 0) {
            stop();
            return;
        }
        int seconds = TurnDeadlineClock.secondsRemaining(state.turnDeadlineEpochMillis);
        if (host.isMyTurn() && !warningShown && seconds == TURN_WARNING_SECONDS) {
            warningShown = true;
            host.onTurnWarning(host.currentPlayerName());
        }
        if (seconds <= 0) {
            stop();
        }
    }

    private void resetWarningIfNewTurn() {
        GameStateDto state = host.state();
        if (state == null) {
            return;
        }
        if (state.currentPlayerIndex != lastTurnIndex) {
            lastTurnIndex = state.currentPlayerIndex;
            warningShown = false;
        }
    }
}
