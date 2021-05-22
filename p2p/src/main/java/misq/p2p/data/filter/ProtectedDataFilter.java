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
import misq.common.util.Tuple2;
import misq.p2p.data.storage.MapKey;
import misq.p2p.data.storage.MapValue;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@EqualsAndHashCode
@Getter
public class ProtectedDataFilter implements DataFilter {
    private final String dataType;
    private final Set<ProtectedDataFilterItem> protectedDataFilterItems;
    private transient Map<MapKey, Tuple2<Integer, Boolean>> map;

    public ProtectedDataFilter(String dataType, Set<ProtectedDataFilterItem> protectedDataFilterItems) {
        this.dataType = dataType;
        this.protectedDataFilterItems = protectedDataFilterItems;
    }

    public void process(Map<MapKey, MapValue> map) {
        Map<MapKey, Tuple2<Integer, Boolean>> filterMap = getMap();
        map.entrySet().stream()
                .filter(entry -> filterMap.containsKey(entry.getKey()))
                .forEach(entry -> {
                    Tuple2<Integer, Boolean> tuple = filterMap.get(entry.getKey());
                    int filterSequenceNumber = tuple.first;
                    boolean wasRemoved = tuple.second;

                    int mySequenceNumber = entry.getValue().getSequenceNumber();
                    if (mySequenceNumber > filterSequenceNumber) {
                        //add
                    }
                });
    }

    @Override
    public boolean matches(MapKey mapKey, MapValue mapValue) {

        return true;
    }

    @NotNull
    private Map<MapKey, Tuple2<Integer, Boolean>> getMap() {
        if (map == null) {
            map = protectedDataFilterItems.stream()
                    .collect(Collectors.toMap(e -> new MapKey(e.getHash()), e -> new Tuple2<>(e.getSequenceNumber(), e.isRemoved())));
        }
        return map;
    }
}
