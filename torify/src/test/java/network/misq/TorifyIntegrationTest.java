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

package network.misq;

import misq.common.util.FileUtils;
import misq.common.util.OsUtils;
import misq.torify.Constants;
import misq.torify.Torify;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class TorifyIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(TorifyIntegrationTest.class);

    @Test
    public void testShutdownDuringStartup() {
        String torDirPath = OsUtils.getUserDataDir() + "/Torify_test";
        File versionFile = new File(torDirPath + "/" + Constants.VERSION);
        FileUtils.deleteDirectory(new File(torDirPath));
        assertFalse(versionFile.exists());
        Torify torify = new Torify(torDirPath);
        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignore) {
            }
            torify.shutdown();

        }).start();
        Thread mainThread = Thread.currentThread();
        torify.startAsync()
                .exceptionally(throwable -> {
                    assertFalse(versionFile.exists());
                    mainThread.interrupt();
                    return null;
                })
                .thenAccept(torController -> {
                    if (torController == null) {
                        return;
                    }
                    fail();
                });
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignore) {
        }
    }

    @Test
    public void testRepeatedStartAndShutdown() throws IOException, InterruptedException {
        String torDirPath = OsUtils.getUserDataDir() + "/Torify_test";
        File versionFile = new File(torDirPath + "/" + Constants.VERSION);
        startAndShutdown(torDirPath, versionFile);
        startAndShutdown(torDirPath, versionFile);
    }

    private void startAndShutdown(String torDirPath, File versionFile) throws IOException, InterruptedException {
        FileUtils.deleteDirectory(new File(torDirPath));
        assertFalse(versionFile.exists());
        Torify torify = new Torify(torDirPath);
        torify.start();
        torify.shutdown();
        assertTrue(versionFile.exists());
    }
}
