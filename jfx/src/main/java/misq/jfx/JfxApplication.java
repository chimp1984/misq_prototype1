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
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import misq.jfx.main.MainView;
import misq.jfx.utils.KeyCodeUtils;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class JfxApplication extends Application {
    public static final CompletableFuture<JfxApplication> LAUNCH_APP_FUTURE = new CompletableFuture<>();
    private JfxApplicationController controller;
    private JfxApplicationModel model;

    private Stage stage;
    @Getter
    private Scene scene;

    public JfxApplication() {
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        LAUNCH_APP_FUTURE.complete(this);
    }

    public void initialize(JfxApplicationModel model, JfxApplicationController controller, MainView mainView) {
        this.controller = controller;
        this.model = model;
        try {
            StackPane root = mainView.getRoot();
            scene = new Scene(root);
            scene.getStylesheets().setAll(getClass().getResource("/misq.css").toExternalForm(),
                    getClass().getResource("/bisq.css").toExternalForm(),
                    getClass().getResource("/theme-dark.css").toExternalForm());

            stage.setScene(scene);
            stage.minHeightProperty().bind(model.minHeightProperty);
            stage.minWidthProperty().bind(model.minWidthProperty);
            stage.titleProperty().bind(model.titleProperty);
            stage.setOnCloseRequest(event -> {
                event.consume();
                controller.onQuit();
            });
            scene.addEventHandler(KeyEvent.KEY_RELEASED, keyEvent -> {
                if (KeyCodeUtils.isCtrlPressed(KeyCode.W, keyEvent) ||
                        KeyCodeUtils.isCtrlPressed(KeyCode.Q, keyEvent)) {
                    controller.onQuit();
                }
            });
            stage.show();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
