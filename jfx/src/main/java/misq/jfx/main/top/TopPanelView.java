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

package misq.jfx.main.top;

import javafx.geometry.Insets;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import misq.jfx.main.top.marketprice.MarketPriceView;
import misq.jfx.utils.ImageUtil;

public class TopPanelView extends HBox {
    public TopPanelView() {
        setMinHeight(80);
        setPadding(new Insets(10, 20, 10, 20));

        ImageView logo = ImageUtil.getImageView("/images/logo_small.png");

        MarketPriceView marketPriceView = new MarketPriceView();

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().addAll(logo, spacer, marketPriceView);
    }
}
