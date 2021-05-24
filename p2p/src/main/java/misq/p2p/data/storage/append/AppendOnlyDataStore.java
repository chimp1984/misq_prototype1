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

package misq.p2p.data.storage.append;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import misq.common.persistence.Persistence;
import misq.common.security.DigestUtil;
import misq.p2p.data.storage.MapKey;
import misq.p2p.data.storage.MetaData;

import java.io.File;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.io.File.separator;

/**
 * Appends AppendOnlyData to a map using the hash of the AppendOnlyData as key.
 * If key already exists we return. If map size exceeds MAX_MAP_SIZE we ignore new data.
 */
@Slf4j
public class AppendOnlyDataStore {
    private static final int MAX_MAP_SIZE = 10_000_000; // in bytes

    private final int maxMapSize;

    public interface Listener {
        void onAppended(AppendOnlyData appendOnlyData);
    }

    private final String storageFilePath;

    final ConcurrentHashMap<MapKey, AppendOnlyData> map = new ConcurrentHashMap<>();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public AppendOnlyDataStore(String storageDirPath, MetaData metaData) {
        this.storageFilePath = storageDirPath + separator + metaData.getFileName();
        maxMapSize = MAX_MAP_SIZE / metaData.getMaxSizeInBytes();
        if (new File(storageFilePath).exists()) {
            Serializable serializable = Persistence.read(storageFilePath);
            if (serializable instanceof ConcurrentHashMap) {
                ConcurrentHashMap<MapKey, AppendOnlyData> persisted = (ConcurrentHashMap<MapKey, AppendOnlyData>) serializable;
                map.putAll(persisted);
            }
        }
    }

    public boolean append(AppendOnlyData appendOnlyData) throws NoSuchAlgorithmException {
        if (map.size() > maxMapSize) {
            return false;
        }

        byte[] hash = DigestUtil.sha256(appendOnlyData.serialize());
        MapKey mapKey = new MapKey(hash);
        if (map.containsKey(mapKey)) {
            return false;
        }

        map.put(mapKey, appendOnlyData);
        listeners.forEach(listener -> listener.onAppended(appendOnlyData));
        persist();
        return true;
    }

    private void persist() {
        Persistence.write(map, storageFilePath);
    }

    public void addListener(AppendOnlyDataStore.Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(AppendOnlyDataStore.Listener listener) {
        listeners.remove(listener);
    }

    @VisibleForTesting
    public ConcurrentHashMap<MapKey, AppendOnlyData> getMap() {
        return map;
    }
}
