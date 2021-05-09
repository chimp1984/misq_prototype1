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

package misq.finance;

import lombok.Getter;

import java.util.List;

@Getter
public class Asset {
    private final String code;
    private final boolean isBase; // True if base currency for price representation
    private final long amount;
    private final List<TransferType> transferTypes; // If not required for protocol its empty

    public Asset(String code, boolean isBase, long amount, List<TransferType> transferTypes) {
        this.code = code;
        this.isBase = isBase;
        this.amount = amount;
        this.transferTypes = transferTypes;
    }
}
