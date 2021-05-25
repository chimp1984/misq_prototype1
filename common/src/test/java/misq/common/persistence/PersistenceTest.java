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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import misq.common.util.FileUtils;
import misq.common.util.OsUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

@Slf4j
public class PersistenceTest {
    @EqualsAndHashCode
    @Getter
    static class MockObject implements Persistable {
        private int index;
        private final ArrayList<Integer> list = new ArrayList<>();

        public MockObject(int index) {
            this.index = index;
        }
    }

    @Test
    public void testPersistence() throws IOException {
        String dir = OsUtils.getUserDataDir() + File.separator + "misq_PersistenceTest";
        FileUtils.makeDirs(dir);
        String storagePath = dir + File.separator + "MockObject";
        MockObject mockObject = new MockObject(1);
        for (int i = 0; i < 100; i++) {
            mockObject.list.add(i);
            Persistence.write(mockObject, storagePath);
        }
        MockObject mockObject2 = (MockObject) Persistence.read(storagePath);
        assertEquals(mockObject.getIndex(), mockObject2.getIndex());
        assertEquals(mockObject.getList(), mockObject2.getList());
    }
}
