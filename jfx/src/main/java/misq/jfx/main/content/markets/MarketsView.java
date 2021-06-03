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

package misq.jfx.main.content.markets;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import misq.jfx.common.View;

public class MarketsView extends View<HBox, MarketsModel, MarketsController> {
    private final Label label;
    private final Button button;

    public MarketsView(MarketsModel model, MarketsController controller) {
        super(new HBox(), model, controller);

        root.setSpacing(20);
        label = new Label();
        button = new Button("Update price");
        root.getChildren().addAll(label, button);

        label.textProperty().bind(model.formattedMarketPrice);
        button.setOnAction(e -> controller.onRefresh());
    }
}
