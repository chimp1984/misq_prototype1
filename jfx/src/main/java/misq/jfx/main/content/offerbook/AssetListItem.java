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

import lombok.Getter;

import java.util.List;

@Getter
public class AssetListItem {
    final String code;
    final boolean isBase; // True if base currency for price representation
    final long amount;
    final List<String> transferTypes; // If not required for protocol its empty

    public AssetListItem(String code, boolean isBase, long amount, List<String> transferTypes) {
        this.code = code;
        this.isBase = isBase;
        this.amount = amount;
        this.transferTypes = transferTypes;
    }
}
