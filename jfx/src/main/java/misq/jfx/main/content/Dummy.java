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

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class Dummy {

    public static class Model implements misq.jfx.common.Model {
    }

    public static class Controller implements misq.jfx.common.Controller {
        @Override
        public void initialize() {
        }

        @Override
        public void onViewAdded() {
        }

        @Override
        public void onViewRemoved() {
        }

        @Override
        public misq.jfx.common.View getView() {
            return null;
        }
    }

    public static class View extends misq.jfx.common.View<StackPane, Model, Controller> {
        public View(String label) {
            super(new StackPane(), new Model(), new Controller());
            root.getChildren().add(new Label(label));
        }
    }
}
