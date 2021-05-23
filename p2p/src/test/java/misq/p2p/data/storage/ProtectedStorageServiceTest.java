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

import lombok.Getter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProtectedStorageServiceTest {
    @Getter
    class MockDataTransaction implements DataTransaction {
        private final int sequenceNumber;
        private final long created;

        public MockDataTransaction(int sequenceNumber, long created) {
            this.sequenceNumber = sequenceNumber;
            this.created = created;
        }
    }

    @Test
    public void testGetSubSet() {
        List<DataTransaction> map = new ArrayList<>();
        map.add(new MockDataTransaction(1, 0));
        int filterOffset = 0;
        int filterRange = 100;
        int maxItems = Integer.MAX_VALUE;
        List<DataTransaction> result = ProtectedStore.getSubSet(map, filterOffset, filterRange, maxItems);
        assertEquals(1, result.size());

        map = new ArrayList<>();
        filterOffset = 0;
        filterRange = 50;
        maxItems = Integer.MAX_VALUE;
        int iterations = 100;
        for (int i = 0; i < iterations; i++) {
            map.add(new MockDataTransaction(i, iterations - i)); // created are inverse order so we can test sorting
        }
        result = ProtectedStore.getSubSet(map, filterOffset, filterRange, maxItems);
        assertEquals(50, result.size());

        filterOffset = 25;
        result = ProtectedStore.getSubSet(map, filterOffset, filterRange, maxItems);
        assertEquals(50, result.size());
        assertEquals(74, result.get(0).getSequenceNumber()); // sorted by date, so list is inverted -> 99-25
        assertEquals(26, result.get(0).getCreated());       // original item i=74 had 100-74=26
        assertEquals(25, result.get(49).getSequenceNumber());

        filterOffset = 85; // 85+50 > 100 -> throw
        try {
            ProtectedStore.getSubSet(map, filterOffset, filterRange, maxItems);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        filterRange = 150; // > 100 -> throw
        filterOffset = 0;
        try {
            ProtectedStore.getSubSet(map, filterOffset, filterRange, maxItems);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        filterOffset = 0;
        filterRange = 100;
        maxItems = 5;
        result = ProtectedStore.getSubSet(map, filterOffset, filterRange, maxItems);
        assertEquals(5, result.size());

        filterOffset = 0;
        filterRange = 100;
        maxItems = 500;
        result = ProtectedStore.getSubSet(map, filterOffset, filterRange, maxItems);
        assertEquals(100, result.size());
    }
}
