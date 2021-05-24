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

package misq.p2p.data.storage;

import misq.common.util.OsUtils;
import misq.p2p.data.storage.append.AppendOnlyDataStore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PermanentStoreTest {
    private String appDirPath = OsUtils.getUserDataDir() + File.separator + "misq_StorageTest";

    @Test
    public void testPermanentStoreTest() throws NoSuchAlgorithmException, IOException {
        MockAppendOnlyData appendOnlyData = new MockAppendOnlyData("test" + UUID.randomUUID().toString());
        AppendOnlyDataStore appendOnlyDataStore = new AppendOnlyDataStore(appDirPath, appendOnlyData.getMetaData());
        int previous = appendOnlyDataStore.getMap().size();
        int iterations = 10;
        for (int i = 0; i < iterations; i++) {
            appendOnlyData = new MockAppendOnlyData("test" + UUID.randomUUID().toString());
            boolean result = appendOnlyDataStore.append(appendOnlyData);
            assertTrue(result);
        }
        assertEquals(iterations + previous, appendOnlyDataStore.getMap().size());
    }
}
