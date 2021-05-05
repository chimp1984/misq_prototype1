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

package misq.common.util;

import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class FileUtil {

    public static void write(String fileName, String data) throws IOException {
        write(fileName, data.getBytes(Charsets.UTF_8));
    }

    public static void write(String fileName, byte[] data) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
            outputStream.write(data);
        } catch (IOException e) {
            throw e;
        }
    }

    public static byte[] read(String fileName) throws IOException {
        return Files.readAllBytes(Paths.get(fileName));
    }

    public static String readAsString(String fileName) throws IOException {
        return new String(read(fileName), Charsets.UTF_8);
    }

    public static void makeDirIfNotExists(String dirName) throws IOException {
        File dir = new File(dirName);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Cannot create directory " + dir.getAbsolutePath());
            }
        }
    }
}
