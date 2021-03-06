/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package misq.jfx.utils;

import javafx.application.Platform;
import misq.common.timer.MisqTimer;
import misq.jfx.utils.reactfx.FxTimer;
import misq.jfx.utils.reactfx.FxTimerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class UITimer implements MisqTimer {
    private final Logger log = LoggerFactory.getLogger(UITimer.class);
    private FxTimer timer;

    public UITimer() {
    }

    @Override
    public MisqTimer runLater(long delay, Runnable runnable) {
        executeDirectlyIfPossible(() -> {
            if (timer == null) {
                timer = FxTimerImpl.create(Duration.ofMillis(delay), runnable);
                timer.restart();
            } else {
                log.warn("runLater called on an already running timer.");
            }
        });
        return this;
    }

    @Override
    public MisqTimer runPeriodically(long interval, Runnable runnable) {
        executeDirectlyIfPossible(() -> {
            if (timer == null) {
                timer = FxTimerImpl.createPeriodic(Duration.ofMillis(interval), runnable);
                timer.restart();
            } else {
                log.warn("runPeriodically called on an already running timer.");
            }
        });
        return this;
    }

    @Override
    public void stop() {
        executeDirectlyIfPossible(() -> {
            if (timer != null) {
                timer.stop();
                timer = null;
            }
        });
    }

    private void executeDirectlyIfPossible(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}
