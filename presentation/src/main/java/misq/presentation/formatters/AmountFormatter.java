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

package misq.presentation.formatters;

import java.text.DecimalFormat;

public class AmountFormatter {
    private static DecimalFormat cryptoFormat = new DecimalFormat("#.####");
    private static DecimalFormat fiatFormat = new DecimalFormat("#.##");

    static {
        cryptoFormat.setMaximumFractionDigits(4);
        fiatFormat.setMaximumFractionDigits(2);
    }

    public static String formatAmount(long amount, String currencyCode) {
        return getFormat(currencyCode).format(amount / getPrecision(currencyCode));
    }

    private static double getPrecision(String currencyCode) {
        return isFiat(currencyCode) ? 10000d : 100000000d;
    }

    private static DecimalFormat getFormat(String currencyCode) {
        return isFiat(currencyCode) ? fiatFormat : cryptoFormat;
    }

    private static boolean isFiat(String currencyCode) {
        switch (currencyCode) {
            case "BTC":
            case "XMR":
            case "USDT":
                return false;
            default:

                return true;
        }
    }
}
