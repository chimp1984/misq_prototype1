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

package misq.jfx.main;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class MarketPriceView extends HBox {

    public final MarketPriceViewModel model;

    public MarketPriceView() {
        setPadding(new Insets(20, 20, 20, 20));
        setSpacing(10);

        model = new MarketPriceViewModel();

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setMinWidth(100);
        comboBox.setItems(model.markets);
        comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                model.onChangeSubscribedMarket(newValue));

        Button button = new Button("Update price");
        button.setOnAction(e -> model.onRefreshMarketPrice());
        Label label = new Label("");
        label.setMinWidth(100);
        HBox.setMargin(label, new Insets(5, 0, 0, 0));
        label.textProperty().bind(model.marketPrice);
        getChildren().addAll(comboBox, label, button);

        model.selectedMarket.addListener((observable, oldValue, newValue) ->
                comboBox.getSelectionModel().select(newValue));
    }
}
