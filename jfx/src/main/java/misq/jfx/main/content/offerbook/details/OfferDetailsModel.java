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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import misq.jfx.common.Model;
import misq.jfx.main.content.offerbook.OfferListItem;

@Getter
public class OfferDetailsModel implements Model {
    private final OfferListItem item;
    DoubleProperty minWidthProperty = new SimpleDoubleProperty(800);
    DoubleProperty minHeightProperty = new SimpleDoubleProperty(400);
    StringProperty titleProperty = new SimpleStringProperty();

    public OfferDetailsModel(OfferListItem item) {
        this.item = item;
        titleProperty.set("Offer " + item.getOffer().getId().substring(0, 8));
    }
}
