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

import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import misq.jfx.common.View;

@Slf4j
public class OfferDetailsView extends View<StackPane, OfferDetailsModel, OfferDetailsController> {
    @org.jetbrains.annotations.NotNull
    private final Bounds boundsInParent;

    public OfferDetailsView(OfferDetailsModel model, OfferDetailsController controller, Bounds boundsInParent) {
        super(new StackPane(), model, controller);
        this.boundsInParent = boundsInParent;

        root.getChildren().add(new Label(model.getItem().toString()));
    }

    protected void onAddedToStage() {
        Scene scene = root.getScene();
        Stage stage = (Stage) scene.getWindow();
        stage.minHeightProperty().bind(model.minHeightProperty);
        stage.minWidthProperty().bind(model.minWidthProperty);
        stage.titleProperty().bind(model.titleProperty);
        stage.setX(boundsInParent.getMinX() - model.minWidthProperty.get() / 2);
        root.getScene().getWindow().setY(boundsInParent.getMinY() + model.minHeightProperty.get() / 2);
    }
}
