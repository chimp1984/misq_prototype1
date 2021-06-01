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

package misq.jfx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import misq.jfx.main.MainView;
import misq.jfx.utils.KeyCodeUtils;
import misq.jfx.utils.UncaughtExceptionHandler;
import misq.jfx.utils.UserThread;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class JfxApplication extends Application {
    public static final CompletableFuture<Boolean> LAUNCH_APP_FUTURE = new CompletableFuture<>();


    private MainView mainView;

    public JfxApplication() {
    }

    @Override
    public void start(Stage stage) {
        try {
            LAUNCH_APP_FUTURE.complete(true);
            mainView = new MainView();

            Scene scene = new Scene(mainView.getRoot());
            scene.getStylesheets().setAll(getClass().getResource("/misq.css").toExternalForm(),
                    getClass().getResource("/bisq.css").toExternalForm(),
                    getClass().getResource("/theme-dark.css").toExternalForm());

            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(1000);
            stage.setTitle("Misq");
            stage.setOnCloseRequest(event -> {
                event.consume();
                stop();
            });
            scene.addEventHandler(KeyEvent.KEY_RELEASED, keyEvent -> {
                if (KeyCodeUtils.isCtrlPressed(KeyCode.W, keyEvent) ||
                        KeyCodeUtils.isCtrlPressed(KeyCode.Q, keyEvent)) {
                    stop();
                }
            });
            stage.show();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void stop() {
        System.exit(0);
    }

    public static void setupUncaughtExceptionHandler(UncaughtExceptionHandler uncaughtExceptionHandler) {
        Thread.UncaughtExceptionHandler handler = (thread, throwable) -> {
            // Might come from another thread
            if (throwable.getCause() != null && throwable.getCause().getCause() != null) {
                log.error(throwable.getMessage());
            } else if (throwable instanceof ClassCastException &&
                    "sun.awt.image.BufImgSurfaceData cannot be cast to sun.java2d.xr.XRSurfaceData".equals(throwable.getMessage())) {
                log.warn(throwable.getMessage());
            } else if (throwable instanceof UnsupportedOperationException &&
                    "The system tray is not supported on the current platform.".equals(throwable.getMessage())) {
                log.warn(throwable.getMessage());
            } else {
                log.error("Uncaught Exception from thread " + Thread.currentThread().getName());
                log.error("throwableMessage= " + throwable.getMessage());
                log.error("throwableClass= " + throwable.getClass());
                // log.error("Stack trace:\n" + ExceptionUtils.getStackTrace(throwable));
                throwable.printStackTrace();
                UserThread.execute(() -> uncaughtExceptionHandler.handleUncaughtException(throwable, false));
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(handler);
        Thread.currentThread().setUncaughtExceptionHandler(handler);
    }
}
