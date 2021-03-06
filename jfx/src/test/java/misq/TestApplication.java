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

package misq;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import misq.jfx.utils.KeyCodeUtils;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class TestApplication extends Application {
    public static CompletableFuture<TestApplication> appLaunchedFuture = new CompletableFuture<>();
    public final Thread javaFXApplicationThread;
    public Pane root;

    public TestApplication() {
        javaFXApplicationThread = Thread.currentThread();
    }

    @Override
    public void start(Stage stage) {
        root = new StackPane();
        Scene scene = new Scene(root);
        scene.getStylesheets().setAll(getClass().getResource("/misq.css").toExternalForm(),
                getClass().getResource("/bisq.css").toExternalForm(),
                getClass().getResource("/theme-dark.css").toExternalForm());

        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(800);
        stage.setTitle("Misq");
        stage.show();

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

        appLaunchedFuture.complete(this);
    }

    @Override
    public void stop() {
        System.exit(0);
    }
}
