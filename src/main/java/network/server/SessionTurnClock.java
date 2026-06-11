package network.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 联机对局的服务端每回合计时调度。
 * 暴露 {@link #deadlineEpochMillis()}，供客户端渲染同步倒计时。
 */
final class SessionTurnClock {
    private final Runnable onTimeout;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "game-session-turn-timer");
        thread.setDaemon(true);
        return thread;
    });

    private ScheduledFuture<?> timeoutTask;
    private long deadlineEpochMillis;
    private boolean paused;
    private long frozenRemainingMillis;

    SessionTurnClock(Runnable onTimeout) {
        this.onTimeout = onTimeout;
    }

    /** 回合计时器暂停且不计时为 true。 */
    boolean isPaused() {
        return paused;
    }

    /** 暂停时的剩余整秒数；未暂停或已过期时为 {@code 0}。 */
    int frozenSecondsRemaining() {
        if (!paused) {
            return 0;
        }
        return (int) Math.ceil(frozenRemainingMillis / 1000.0);
    }

    /** 当前回合结束的纪元毫秒时间戳；无计时器运行时为 0。 */
    long deadlineEpochMillis() {
        return deadlineEpochMillis;
    }

    /** 在 {@code turnTimeSeconds} 后调度 {@link #onTimeout} 并记录截止时间。 */
    void start(int turnTimeSeconds) {
        cancel();
        deadlineEpochMillis = System.currentTimeMillis() + turnTimeSeconds * 1000L;
        timeoutTask = executor.schedule(onTimeout, turnTimeSeconds, TimeUnit.SECONDS);
    }

    /** 冻结倒计时，直至调用 {@link #resume()}。 */
    void pause() {
        if (paused || timeoutTask == null) {
            return;
        }
        frozenRemainingMillis = Math.max(0, deadlineEpochMillis - System.currentTimeMillis());
        timeoutTask.cancel(false);
        timeoutTask = null;
        paused = true;
    }

    /** 从 {@link #pause()} 保存的剩余时间重新启动倒计时。 */
    void resume() {
        if (!paused) {
            return;
        }
        paused = false;
        if (frozenRemainingMillis <= 0) {
            deadlineEpochMillis = 0;
            onTimeout.run();
            return;
        }
        deadlineEpochMillis = System.currentTimeMillis() + frozenRemainingMillis;
        timeoutTask = executor.schedule(onTimeout, frozenRemainingMillis, TimeUnit.MILLISECONDS);
        frozenRemainingMillis = 0;
    }

    void cancel() {
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
            timeoutTask = null;
        }
        deadlineEpochMillis = 0;
        paused = false;
        frozenRemainingMillis = 0;
    }

    void shutdown() {
        cancel();
        executor.shutdownNow();
    }
}
