package misq.jfx.utils.reactfx;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * Provides factory methods for timers that are manipulated from and execute
 * their action on the JavaFX application thread.
 * <p>
 * Copied from:
 * https://github.com/TomasMikula/ReactFX/blob/537fffdbb2958a77dfbca08b712bb2192862e960/reactfx/src/main/java/org/reactfx/util/FxTimer.java
 */
public class FxTimerImpl implements FxTimer {

    /**
     * Prepares a (stopped) timer that lasts for {@code delay} and whose action runs when timer <em>ends</em>.
     */
    public static FxTimer create(java.time.Duration delay, Runnable action) {
        return new FxTimerImpl(delay, delay, action, 1);
    }

    /**
     * Equivalent to {@code create(delay, action).restart()}.
     */
    public static FxTimer runLater(java.time.Duration delay, Runnable action) {
        FxTimer timer = create(delay, action);
        timer.restart();
        return timer;
    }

    /**
     * Prepares a (stopped) timer that lasts for {@code interval} and that executes the given action periodically
     * when the timer <em>ends</em>.
     */
    public static FxTimer createPeriodic(java.time.Duration interval, Runnable action) {
        return new FxTimerImpl(interval, interval, action, Animation.INDEFINITE);
    }

    /**
     * Equivalent to {@code createPeriodic(interval, action).restart()}.
     */
    public static FxTimer runPeriodically(java.time.Duration interval, Runnable action) {
        FxTimer timer = createPeriodic(interval, action);
        timer.restart();
        return timer;
    }

    /**
     * Prepares a (stopped) timer that lasts for {@code interval} and that executes the given action periodically
     * when the timer <em>starts</em>.
     */
    public static FxTimer createPeriodic0(java.time.Duration interval, Runnable action) {
        return new FxTimerImpl(java.time.Duration.ZERO, interval, action, Animation.INDEFINITE);
    }

    /**
     * Equivalent to {@code createPeriodic0(interval, action).restart()}.
     */
    public static FxTimer runPeriodically0(java.time.Duration interval, Runnable action) {
        FxTimer timer = createPeriodic0(interval, action);
        timer.restart();
        return timer;
    }

    private final Duration actionTime;
    private final Timeline timeline;
    private final Runnable action;

    private long seq = 0;

    private FxTimerImpl(java.time.Duration actionTime, java.time.Duration period, Runnable action, int cycles) {
        this.actionTime = Duration.millis(actionTime.toMillis());
        this.timeline = new Timeline();
        this.action = action;

        timeline.getKeyFrames().add(new KeyFrame(this.actionTime)); // used as placeholder
        if (period != actionTime) {
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(period.toMillis())));
        }

        timeline.setCycleCount(cycles);
    }

    @Override
    public void restart() {
        stop();
        long expected = seq;
        timeline.getKeyFrames().set(0, new KeyFrame(actionTime, ae -> {
            if (seq == expected) {
                action.run();
            }
        }));
        timeline.play();
    }

    @Override
    public void stop() {
        timeline.stop();
        ++seq;
    }
}
