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

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import misq.finance.swap.offer.SwapOffer;
import misq.presentation.offer.OfferEntity;

public class OfferListItem extends OfferEntity {
    private final DoubleProperty priceAsDoubleProperty = new SimpleDoubleProperty(0d);
    @Getter
    private final StringProperty priceProperty = new SimpleStringProperty("");
    @Getter
    private final StringProperty quoteAmountProperty = new SimpleStringProperty("");

    public OfferListItem(SwapOffer offer, BehaviorSubject<Double> marketPriceSubject) {
        super(offer, marketPriceSubject);
    }

    protected void updatedPriceAndAmount(double marketPrice) {
        super.updatedPriceAndAmount(marketPrice);
        // We get called from the constructor of our superclass, so our fields are not initialized at that moment.
        // We delay with Platform.runLater which guards us also in case we get called from a non JavaFxApplication thread.
        Platform.runLater(() -> {
            priceProperty.set(formattedPrice);
            priceAsDoubleProperty.set(price);
            quoteAmountProperty.set(formattedQuoteAmount);
        });
    }
}
