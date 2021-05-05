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

import java.io.File;
import java.nio.file.Paths;
import java.util.Locale;

public class OsUtils {
    public static File getUserDataDir() {
        if (isWindows())
            return new File(System.getenv("APPDATA"));

        if (isOSX())
            return Paths.get(System.getProperty("user.home"), "Library", "Application Support").toFile();

        // *nix
        return Paths.get(System.getProperty("user.home"), ".local", "share").toFile();
    }

    public static boolean isWindows() {
        return getOSName().contains("win");
    }

    public static boolean isOSX() {
        return getOSName().contains("mac") || getOSName().contains("darwin");
    }

    private static String getOSName() {
        return System.getProperty("os.name").toLowerCase(Locale.US);
    }

}
