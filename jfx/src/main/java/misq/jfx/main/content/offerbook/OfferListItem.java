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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class OfferListItem {
    private final BooleanProperty isVisible = new SimpleBooleanProperty(false);
    private final String amount;
    private final double premium;
    private final StringProperty price;
    private final String maker;
    private final String details;

    public OfferListItem(double premium, String amount, StringProperty price, String maker, String details) {
        this.premium = premium;
        this.amount = amount;
        this.price = price;
        this.maker = maker;
        this.details = details;
    }
}
