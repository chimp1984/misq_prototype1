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

package misq.finance.offer;

import lombok.EqualsAndHashCode;
import lombok.Getter;

// For some fiat methods is useful to know the makers bank or county of bank.
@Getter
@EqualsAndHashCode
public class TransferOptions {
    private final String countyCodeOfBank;
    private final String bankName;

    public TransferOptions(String countyCodeOfBank, String bankName) {
        this.countyCodeOfBank = countyCodeOfBank;
        this.bankName = bankName;
    }
}
