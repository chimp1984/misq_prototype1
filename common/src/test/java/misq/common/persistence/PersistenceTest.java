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

package misq.common.persistence;

import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
public class PersistenceTest {
    @EqualsAndHashCode
    static class MockObject implements Persistable {
        int i;
        ArrayList<Integer> list = new ArrayList<>();

        public MockObject(int i) {
            this.i = i;
        }

        public String getDefaultStorageFileName() {
            return this.getClass().getSimpleName();
        }
    }

    @Test
    public void testPersistence() throws IOException {
        MockObject mockObject = new MockObject(1);
        long ts = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            mockObject.list.add(i);
            Persistence.write(mockObject);
        }
        log.error(" {}", System.currentTimeMillis() - ts);

        //   MockObject mockObject2 = (MockObject) Persistence.read(MockObject.class.getSimpleName());
        //  log.error(" {}", mockObject2.list);
        //   assertEquals(mockObject.i, mockObject2.i);
    }
}
