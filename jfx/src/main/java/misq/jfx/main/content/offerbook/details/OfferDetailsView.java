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

package misq.jfx.main.content.offerbook.details;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;
import misq.jfx.common.View;

@Slf4j
public class OfferDetailsView extends View<StackPane> {
    @org.jetbrains.annotations.NotNull
    private final OfferDetailsModel model;
    private final OfferDetailsController offerDetailsController;
    private final Bounds boundsInParent;

    public OfferDetailsView(OfferDetailsModel model, OfferDetailsController offerDetailsController, Bounds boundsInParent) {
        super(new StackPane());
        this.model = model;
        this.offerDetailsController = offerDetailsController;
        this.boundsInParent = boundsInParent;

        root.getChildren().add(new Label(model.getItem().toString()));
    }

    protected void onAddedToStage() {
        Scene scene = root.getScene();
        scene.windowProperty().addListener(new ChangeListener<Window>() {
            @Override
            public void changed(ObservableValue<? extends Window> observable, Window oldValue, Window newValue) {
                if (newValue != null) {
                    Stage stage = (Stage) scene.getWindow();
                    stage.minHeightProperty().bind(model.minHeightProperty);
                    stage.minWidthProperty().bind(model.minWidthProperty);
                    stage.titleProperty().bind(model.titleProperty);
                    stage.setX(boundsInParent.getMinX() - model.minWidthProperty.get() / 2);
                    root.getScene().getWindow().setY(boundsInParent.getMinY() + model.minHeightProperty.get() / 2);
                }
            }
        });

    }
}
