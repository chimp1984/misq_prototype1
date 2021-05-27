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
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import misq.jfx.main.MarketPriceView;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class JfxApplication extends Application {
    public static CompletableFuture<JfxApplication> appLaunchedFuture = new CompletableFuture<>();
    public final Thread javaFXApplicationThread;
    public MarketPriceView balancesView;

    public JfxApplication() {
        javaFXApplicationThread = Thread.currentThread();
    }

    @Override
    public void start(Stage primaryStage) {
        balancesView = new MarketPriceView();
        Scene scene = new Scene(balancesView);

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(1000);
        primaryStage.setTitle("Misq");
        primaryStage.show();

        appLaunchedFuture.complete(this);
    }

    @Override
    public void stop() {
        System.exit(0);
    }
}
