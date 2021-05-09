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

package misq.torify;

import java.io.IOException;
import java.util.Scanner;

public enum OsType {
    WIN,
    LNX32,
    LNX64,
    MACOS;

    public static OsType detectOs() {
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows")) {
            return WIN;
        } else if (osName.contains("Mac")) {
            return MACOS;
        } else if (osName.contains("Linux")) {
            return detectLinux();
        }
        return null;
    }

    private static OsType detectLinux() {
        String[] cmd = new String[]{"uname", "-m"};
        try {
            Process unameProcess = Runtime.getRuntime().exec(cmd);
            try (Scanner scanner = new Scanner(unameProcess.getInputStream())) {
                String info = null;
                while (scanner.hasNext()) {
                    info = scanner.nextLine();
                }

                int exit = unameProcess.waitFor();
                if (exit != 0) {
                    throw new RuntimeException("Uname returned error code " + exit);
                }

                if (info == null) {
                    throw new RuntimeException("Could not resolve Linux version");
                }

                if (info.contains("i.86")) {
                    return LNX32;
                } else if (info.contains("x86_64")) {
                    return LNX64;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Could not resolve Linux version due exception. " + e.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException("uname failed. " + e.toString());
        }
        return null;
    }

    public String getArchiveName() {
        switch (this) {
            case WIN:
                return "tor.exe";//todo
            case LNX32:
            case LNX64:
                return "tor";//todo
            case MACOS:
                return "/native/osx/x64/tor.tar.xz";
            default:
                throw new RuntimeException("We don't support Tor on this OS");
        }
    }

    public String getTorrcNative() {
        switch (this) {
            case WIN:
                return "tor.exe"; //todo
            case LNX32:
            case LNX64:
                return "tor"; //todo
            case MACOS:
                return "/native/osx/" + Constants.TORRC_NATIVE;
            default:
                throw new RuntimeException("We don't support Tor on this OS");
        }
    }

    public String getBinaryName() {
        switch (this) {
            case WIN:
                return "tor.exe";
            case LNX32:
            case LNX64:
                return "tor";
            case MACOS:
                return "tor.real";
            default:
                throw new RuntimeException("We don't support Tor on this OS");
        }
    }
}
