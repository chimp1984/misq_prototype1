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
import lombok.extern.slf4j.Slf4j;
import misq.api.Api;
import misq.jfx.main.MainViewController;
import misq.jfx.utils.UncaughtExceptionHandler;
import misq.jfx.utils.UserThread;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class JfxApplicationController {
    private final Api api;
    private final JfxApplicationModel model;
    private JfxApplication jfxApplication;
    private MainViewController mainViewController;

    public JfxApplicationController(Api api) {
        this.api = api;
        this.model = new JfxApplicationModel();
    }

    public CompletableFuture<Boolean> launchApplication() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JfxApplication.LAUNCH_APP_FUTURE.whenComplete((jfxApplication, throwable) -> {
            if (jfxApplication != null) {
                this.jfxApplication = jfxApplication;
                mainViewController = new MainViewController(api);
                jfxApplication.initialize(model, this, mainViewController.getView());
                future.complete(true);
            } else {
                throwable.printStackTrace();
            }
        });
        new Thread(() -> {
            Thread.currentThread().setName("JfxApplicationLauncher");
            Application.launch(JfxApplication.class); //blocks until app closed
        }).start();
        return future;
    }

    public void onQuit() {
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