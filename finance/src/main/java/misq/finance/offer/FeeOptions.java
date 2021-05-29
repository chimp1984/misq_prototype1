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

// Data for verifying fee payment. Open question how we deal with fees...
@Getter
@EqualsAndHashCode
public class FeeOptions {
    public enum Type {
        BTC,
        BSQ
    }

    private final Type type;
    private final int blockHeightAtFeePayment;
    private final String feeTxId;

    public FeeOptions(Type type, int blockHeightAtFeePayment, String feeTxId) {
        this.type = type;
        this.blockHeightAtFeePayment = blockHeightAtFeePayment;
        this.feeTxId = feeTxId;
    }
}
