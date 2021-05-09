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

import java.util.List;

public class Constants {
    public final static String LOCALHOST = "127.0.0.1";

    // directories
    public final static String DOT_TOR = ".tor";
    public final static String HS_DIR = "hiddenservice";

    // files
    public final static String VERSION = "version";
    public final static String GEO_IP = "geoip";
    public final static String GEO_IPV_6 = "geoip6";
    public final static String TORRC = "torrc";
    public final static String PID = "pid";
    public final static String COOKIE = "control_auth_cookie";
    public final static String HOSTNAME = "hostname";
    public final static String PRIV_KEY = "private_key";

    // resources
    public final static String TORRC_DEFAULTS = "torrc.defaults";
    public final static String TORRC_NATIVE = "torrc.native";

    // torrc keys
    public final static String TORRC_KEY_GEOIP6 = "GeoIPv6File";
    public final static String TORRC_KEY_GEOIP = "GeoIPFile";
    public final static String TORRC_KEY_PID = "PidFile";
    public final static String TORRC_KEY_DATA_DIRECTORY = "DataDirectory";
    public final static String TORRC_KEY_COOKIE = "CookieAuthFile";

    // hs
    public final static String HIDDEN_SERVICE_NAME = "hiddenservice";

    // control
    public final static String LOG_OF_CONTROL_PORT = "Control listener listening on port ";
    // public final static List<String> EVENTS = List.of("CIRC", "WARN", "ERR"); netlayer
    public final static List<String> EVENTS2 = List.of("CIRC", "WARN", "ERR");
    public final static List<String> EVENTS1 = List.of("CIRC", "ORCONN", "HS_DESC", "NOTICE", "WARN", "ERR"); // briar
    public final static List<String> EVENTS = List.of("INFO", "NOTICE", "WARN", "ERR", "CIRC", "ORCONN", "HS_DESC", "HS_DESC_CONTENT");

    public final static String STATUS_BOOTSTRAP_PHASE = "status/bootstrap-phase";
    public final static String DISABLE_NETWORK = "DisableNetwork";
    public final static String NET_LISTENERS_SOCKS = "net/listeners/socks";
    // tor process
    public final static String OWNER = "__OwningControllerProcess";
}
