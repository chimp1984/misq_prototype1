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
import javafx.beans.property.ObjectProperty;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.RangeSlider;

import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class RangeSliderBox extends Pane {
    private final RangeSlider slider;
    private final Label titleLabel, minLabel, maxLabel, lowLabel, highLabel;
    private final ObjectProperty<Number> lowValueProperty, highValueProperty;
    private final FilteredList<OfferItem> filteredItems;
    private final Supplier<Number> minValueSupplier, maxValueSupplier;
    private final Function<Number, String> formatter;
    private boolean highValueSet, lowValueSet;
    private Number minValue, maxValue;
    private InvalidationListener filteredItemsListener;

    public RangeSliderBox(String title,
                          int width,
                          ObjectProperty<Number> lowValueProperty,
                          ObjectProperty<Number> highValueProperty,
                          FilteredList<OfferItem> filteredItems,
                          Supplier<Number> minValueSupplier,
                          Supplier<Number> maxValueSupplier,
                          Function<Number, String> formatter) {
        //setStyle("-fx-background-color: blue;");
        setPrefWidth(width);
        this.lowValueProperty = lowValueProperty;
        this.highValueProperty = highValueProperty;
        this.filteredItems = filteredItems;
        this.minValueSupplier = minValueSupplier;
        this.maxValueSupplier = maxValueSupplier;
        this.formatter = formatter;

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
    }

    public void onViewAdded() {
        slider.applyCss();
        Pane lowThumb = (Pane) slider.lookup(".range-slider .low-thumb");
        log.error("lowThumb " + lowThumb);
        lowLabel.layoutXProperty().bind(lowThumb.layoutXProperty().add(0));
        lowLabel.layoutYProperty().bind(lowThumb.layoutYProperty().subtract(25));
        Pane highThumb = (Pane) slider.lookup(".range-slider .high-thumb");
        highLabel.layoutXProperty().bind(highThumb.layoutXProperty().add(highThumb.widthProperty()).subtract(highLabel.widthProperty()));
        highLabel.layoutYProperty().bind(highThumb.layoutYProperty().subtract(25));

        minLabel.layoutYProperty().bind(slider.layoutYProperty().add(20));
        maxLabel.layoutXProperty().bind(slider.widthProperty().subtract(maxLabel.widthProperty()));
        maxLabel.layoutYProperty().bind(slider.layoutYProperty().add(20));

        titleLabel.layoutXProperty().bind(slider.widthProperty().subtract(titleLabel.widthProperty()).divide(2));
        titleLabel.layoutYProperty().bind(slider.layoutYProperty().subtract(45));

        filteredItemsListener = observable -> update();
        filteredItems.addListener(filteredItemsListener);
        update();
    }

    private void update() {
        updateMin();
        updateMax();
    }

    private void updateMin() {
        minValue = minValueSupplier.get();

        if (!lowValueSet) {
            lowValueSet = true;
            slider.lowValueProperty().addListener((observable, oldValue, newValue) -> {
                Number value;
                if (maxValue instanceof Long) {
                    long maxValueAsLong = (long) maxValue;
                    long minValueAsLong = (long) minValue;
                    value = minValueAsLong + Math.round((maxValueAsLong - minValueAsLong) * ((double) newValue) / 100);
                } else {
                    double maxValueAsDouble = (double) maxValue;
                    double minValueAsDouble = (double) minValue;
                    value = minValueAsDouble + Math.round((maxValueAsDouble - minValueAsDouble) * ((double) newValue) / 100);
                }
                filteredItems.removeListener(filteredItemsListener);
                lowLabel.setText(formatter.apply(value));
                lowValueProperty.set(value);
                filteredItems.addListener(filteredItemsListener);
            });
        }
        String formatted = formatter.apply(minValue);
        lowLabel.setText(formatted);
    }

    private void updateMax() {
        maxValue = maxValueSupplier.get();

        if (!highValueSet) {
            highValueSet = true;

            slider.highValueProperty().addListener((observable, oldValue, newValue) -> {
                Number value;
                if (maxValue instanceof Long) {
                    long maxValueAsLong = (long) maxValue;
                    value = Math.round(maxValueAsLong * ((double) newValue) / 100);
                } else {
                    double maxValueAsDouble = (double) maxValue;
                    value = Math.round(maxValueAsDouble * ((double) newValue) / 100);
                }
                highLabel.setText(formatter.apply(value));
                filteredItems.removeListener(filteredItemsListener);
                highValueProperty.set(value);
                filteredItems.addListener(filteredItemsListener);
            });
        }

        String formatted = formatter.apply(maxValue);
        highLabel.setText(formatted);
    }
}
