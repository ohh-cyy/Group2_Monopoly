package controller.local;

import engine.GameEngine;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import model.player.Player;

/**
 * 本地热座游戏的每回合倒计时。
 * <p>
 * 在 {@link #TURN_WARNING_SECONDS} 时触发警告，并将超时处理委托给
 * {@link Host}，以便控制器跳过当前玩家并推进回合。
 */
public final class LocalTurnTimer {
    /** 自动跳过前每回合允许的总秒数。 */
    public static final int TURN_TIME_SECONDS = 60;
    /** 显示一次性警告时的剩余秒数。 */
    public static final int TURN_WARNING_SECONDS = 10;

    /**
     * 计时器调用的回调；通常由 {@link controller.GameController} 实现。
     */
    public interface Host {
        /** 用于检测游戏结束并识别当前玩家的引擎。 */
        GameEngine engine();

        /** 每秒调用，供 UI 刷新倒计时标签。 */
        void onTimerTick(int secondsRemaining);

        /** 到达 {@link #TURN_WARNING_SECONDS} 时调用一次。 */
        void onTurnWarning(Player currentPlayer);

        /** 倒计时归零时调用；宿主应跳过该玩家。 */
        void onTurnTimedOut(Player skipped);
    }

    private final Host host;
    private Timeline timeline;
    private int secondsRemaining = TURN_TIME_SECONDS;
    private boolean warningShown;

    /**
     * @param host 滴答、警告与超时处理的回调
     */
    public LocalTurnTimer(Host host) {
        this.host = host;
    }

    /** 当前回合剩余秒数（超时或游戏结束后为 0）。 */
    public int getSecondsRemaining() {
        return secondsRemaining;
    }

    /** 为新回合重启倒计时；游戏已结束则停止。 */
    public void reset() {
        stop();
        GameEngine engine = host.engine();
        if (engine == null || engine.isGameOver()) {
            secondsRemaining = 0;
            return;
        }
        secondsRemaining = TURN_TIME_SECONDS;
        warningShown = false;
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    /** 停止 JavaFX 时间线，不改变 {@link #secondsRemaining}。 */
    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    /** 游戏暂停时暂停倒计时；未运行则无操作。 */
    public void pause() {
        if (timeline != null) {
            timeline.pause();
        }
    }

    /** 恢复已暂停的倒计时；未运行则无操作。 */
    public void resume() {
        if (timeline != null) {
            timeline.play();
        }
    }

    private void tick() {
        GameEngine engine = host.engine();
        if (engine == null || engine.isGameOver()) {
            stop();
            return;
        }
        secondsRemaining = Math.max(0, secondsRemaining - 1);
        if (!warningShown && secondsRemaining == TURN_WARNING_SECONDS) {
            warningShown = true;
            host.onTurnWarning(engine.getCurrentPlayer());
        }
        host.onTimerTick(secondsRemaining);
        if (secondsRemaining == 0) {
            stop();
            host.onTurnTimedOut(engine.getCurrentPlayer());
        }
    }
}
