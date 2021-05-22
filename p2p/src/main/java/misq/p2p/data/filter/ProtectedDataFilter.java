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

package misq.p2p.data.filter;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import misq.p2p.data.storage.DataTransaction;
import misq.p2p.data.storage.MapKey;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@EqualsAndHashCode
@Getter
public class ProtectedDataFilter implements DataFilter {
    private final String dataType;
    private final Set<ProtectedDataFilterItem> filterItems;
    transient private final Map<MapKey, Integer> filterMap;

    public ProtectedDataFilter(String dataType, Set<ProtectedDataFilterItem> filterItems) {
        this.dataType = dataType;
        this.filterItems = filterItems;
        filterMap = filterItems.stream()
                .collect(Collectors.toMap(e -> new MapKey(e.getHash()), ProtectedDataFilterItem::getSequenceNumber));
    }

    public Set<DataTransaction> filter(Map<MapKey, DataTransaction> storageMap) {
        return storageMap.entrySet().stream()
                .filter(entry -> filterMap.containsKey(entry.getKey()))
                .filter(entry -> entry.getValue().getSequenceNumber() > filterMap.get(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }

}
