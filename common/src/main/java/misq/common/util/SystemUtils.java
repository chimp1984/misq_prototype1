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
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Random;

public class SystemUtils {
    public static String getOSName() {
        return System.getProperty("os.name").toLowerCase(Locale.US);
    }

    public static boolean isWindows() {
        return getOSName().contains("win");
    }

    public static boolean isOSX() {
        return getOSName().contains("mac") || getOSName().contains("darwin");
    }

    public static File getUserDataDir() {
        if (isWindows())
            return new File(System.getenv("APPDATA"));

        if (isOSX())
            return Paths.get(System.getProperty("user.home"), "Library", "Application Support").toFile();

        // *nix
        return Paths.get(System.getProperty("user.home"), ".local", "share").toFile();
    }

    public static int findFreeSystemPort() {
        try {
            ServerSocket server = new ServerSocket(0);
            int port = server.getLocalPort();
            server.close();
            return port;
        } catch (IOException ignored) {
            return new Random().nextInt(10000) + 50000;
        }
    }
}
