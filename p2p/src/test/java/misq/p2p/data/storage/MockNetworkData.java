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

import lombok.EqualsAndHashCode;
import misq.p2p.NetworkData;

import java.util.concurrent.TimeUnit;

@EqualsAndHashCode
public class MockNetworkData implements NetworkData {
    private final String text;

    public MockNetworkData(String text) {
        this.text = text;
    }

    @Override
    public String getFileName() {
        return "MockNetworkData";
    }

    @Override
    public long getTTL() {
        return TimeUnit.SECONDS.toMillis(10);
    }

    @Override
    public boolean isDataInvalid() {
        return false;
    }
}
