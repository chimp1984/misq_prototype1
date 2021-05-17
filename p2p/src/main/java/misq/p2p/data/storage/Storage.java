/*
 * This file is part of Misq.
 *
 * Misq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Misq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Misq. If not, see <http://www.gnu.org/licenses/>.
 */

package misq.p2p.data.storage;


import misq.p2p.data.filter.DataFilter;
import misq.p2p.data.inventory.Inventory;
import misq.p2p.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class Storage {
    private static final Logger log = LoggerFactory.getLogger(Storage.class);

    private final Map<MapKey, Message> map = new ConcurrentHashMap<>();

    public Storage() {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Message add(Message message) {
        return map.put(new MapKey(message), message);
    }

    public Message remove(MapKey mapKey) {
        return map.remove(mapKey);
    }

    public Message getInventory(MapKey mapKey) {
        return map.get(mapKey);
    }

    public Inventory getInventory(DataFilter dataFilter) {
        return new Inventory(map.entrySet().stream()
                .filter(e -> dataFilter.matches(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet()));
    }

    public Collection<Message> getAll() {
        return map.values();
    }


    public CompletableFuture<Inventory> add(Inventory inventory) {
        return CompletableFuture.supplyAsync(() -> {
            inventory.getCollection().forEach(this::add);
            return inventory;
        });
    }

    public void shutdown() {

    }
}
