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

package misq.p2p.data.storage.mailbox;

import misq.common.util.FileUtils;
import misq.p2p.data.storage.MetaData;
import misq.p2p.data.storage.Storage;

import java.io.File;
import java.io.IOException;

import static java.io.File.separator;

public abstract class DataStore {
    protected final String storageFilePath;

    public DataStore(String appDirPath, MetaData metaData) throws IOException {
        String dir = appDirPath + Storage.DIR + File.separator + getStoreDir();
        FileUtils.makeDirs(dir);
        storageFilePath = dir + separator + metaData.getFileName();
    }

    protected String getStoreDir() {
        return this.getClass().getSimpleName().replace("DataStore", "").toLowerCase();
    }

    abstract public void shutdown();
}
