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

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiFunction;

@Slf4j
@Getter
public class OfferItemOld {
    private final String id;
    private final String amount;
    private final double premium;
    private final StringProperty price;
    private final DoubleProperty marketPrice;
    private final String maker;
    private final String details;
    private final ChangeListener<Number> listener;
    private final BiFunction<Double, Double, String> marketPriceFormatter;

    public OfferItemOld(String id,
                        double premium,
                        String amount,
                        StringProperty price,
                        String maker,
                        String details,
                        DoubleProperty marketPrice,
                        BiFunction<Double, Double, String> marketPriceFormatter) {
        this.id = id;
        this.premium = premium;
        this.amount = amount;
        this.price = price;
        this.maker = id.substring(0, 5);
        this.details = details;
        this.marketPrice = marketPrice;

        // We get called from a non UI thread so we need to wrap it into Platform.runLater
        listener = (observable, oldValue, newValue) -> Platform.runLater(this::applyPrice);
        this.marketPriceFormatter = marketPriceFormatter;
        applyPrice();
    }

    private void applyPrice() {
        price.set(marketPriceFormatter.apply(premium, marketPrice.get()));
    }

    public void isVisible(boolean visible) {
        if (visible) {
            applyPrice();
            marketPrice.addListener(listener);
        } else {
            marketPrice.removeListener(listener);
        }
    }
}
