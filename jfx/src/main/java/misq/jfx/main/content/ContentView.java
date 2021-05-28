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

package misq.jfx.main.content;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import misq.jfx.main.MainView;
import misq.jfx.navigation.Navigation;
import misq.jfx.navigation.ViewLoader;

import java.lang.reflect.InvocationTargetException;

public class ContentView extends HBox {
    public ContentView() {
        setStyle("-fx-background-color: blue;");
        Navigation.addListener((viewPath, data) -> {
            if (viewPath.size() != 2 || viewPath.indexOf(MainView.class) != 0)
                return;

            Class<? extends Node> viewClass = viewPath.tip();
            if (viewClass == null) {
                return;
            }
            try {
                Node view = ViewLoader.load(viewClass);
                HBox.setHgrow(view, Priority.ALWAYS);
                getChildren().setAll(view);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                e.printStackTrace();
            }
        });
    }
}
