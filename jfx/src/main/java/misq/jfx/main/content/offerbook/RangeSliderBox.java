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

package misq.jfx.main.content.offerbook;

import javafx.beans.InvalidationListener;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;
import misq.presentation.formatters.AmountFormatter;
import misq.presentation.offer.OfferbookController;
import misq.presentation.offer.OfferbookModel;
import org.controlsfx.control.RangeSlider;

@Slf4j
public class RangeSliderBox extends Pane {
    private final RangeSlider slider;
    private final Label titleLabel, minLabel, maxLabel, lowLabel, highLabel;
    private final OfferbookModel model;
    private final OfferbookController controller;
    private final InvalidationListener filteredItemsListener;
    private boolean highValueSet, lowValueSet;

    public RangeSliderBox(String title, int width, OfferbookModel model, OfferbookController controller) {
        this.model = model;
        this.controller = controller;
        //setStyle("-fx-background-color: blue;");
        setPrefWidth(width);

        titleLabel = new Label(title);
        minLabel = new Label("Min");
        maxLabel = new Label("Max");
        lowLabel = new Label("lowLabel");
        highLabel = new Label("highLabel");
        slider = new RangeSlider(0, 100, 0, 100);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        slider.prefWidthProperty().bind(this.prefWidthProperty());
        getChildren().addAll(titleLabel, slider, minLabel, maxLabel, lowLabel, highLabel);
        setMaxHeight(50);
        filteredItemsListener = observable -> update();
    }

    public void onViewAdded() {
        slider.applyCss();
        Pane lowThumb = (Pane) slider.lookup(".range-slider .low-thumb");
        lowLabel.layoutXProperty().bind(lowThumb.layoutXProperty());
        lowLabel.layoutYProperty().bind(lowThumb.layoutYProperty().subtract(25));
        Pane highThumb = (Pane) slider.lookup(".range-slider .high-thumb");
        highLabel.layoutXProperty().bind(highThumb.layoutXProperty().add(highThumb.widthProperty()).subtract(highLabel.widthProperty()));
        highLabel.layoutYProperty().bind(highThumb.layoutYProperty().subtract(25));

        minLabel.layoutYProperty().bind(slider.layoutYProperty().add(20));
        maxLabel.layoutXProperty().bind(slider.widthProperty().subtract(maxLabel.widthProperty()));
        maxLabel.layoutYProperty().bind(slider.layoutYProperty().add(20));

        titleLabel.layoutXProperty().bind(slider.widthProperty().subtract(titleLabel.widthProperty()).divide(2));
        titleLabel.layoutYProperty().bind(slider.layoutYProperty().subtract(45));

        model.getFilteredItems().addListener(filteredItemsListener);
        update();
    }

    private void update() {
        updateMin();
        updateMax();
    }

    protected void updateMin() {
        long minValue = model.getMinBaseAmountValue();
        lowLabel.setText(AmountFormatter.formatAmount(minValue, model.getBaseCurrency()));

        if (!lowValueSet) {
            lowValueSet = true;
            slider.lowValueProperty().addListener((observable, oldValue, newValue) -> {
                model.getFilteredItems().removeListener(filteredItemsListener);
                controller.onLowBaseAmountFilterChange((double) newValue);
                lowLabel.setText(AmountFormatter.formatAmount(model.getLowBaseAmountValue(), model.getBaseCurrency()));
                model.getFilteredItems().addListener(filteredItemsListener);
            });
        }
    }

    protected void updateMax() {
        long maxValue = model.getMaxBaseAmountValue();
        highLabel.setText(AmountFormatter.formatAmount(maxValue, model.getBaseCurrency()));

        if (!highValueSet) {
            highValueSet = true;
            slider.highValueProperty().addListener((observable, oldValue, newValue) -> {
                model.getFilteredItems().removeListener(filteredItemsListener);
                controller.onHighBaseAmountFilterChange((double) newValue);
                highLabel.setText(AmountFormatter.formatAmount(model.getHighBaseAmountValue(), model.getBaseCurrency()));
                model.getFilteredItems().addListener(filteredItemsListener);
            });
        }
    }
}
