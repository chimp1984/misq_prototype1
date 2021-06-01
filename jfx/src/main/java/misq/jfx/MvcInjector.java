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

package misq.jfx;

import lombok.extern.slf4j.Slf4j;
import misq.jfx.common.View;
import misq.presentation.Controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Just temp to have some simple injection feature...
@Slf4j
public class MvcInjector {
    private final static Map<Class<? extends View>, Controller> map = new ConcurrentHashMap<>();

    public static void glue(Class<? extends View> clazz, Controller controller) {
        map.put(clazz, controller);
    }

    public static <M extends Controller> M getController(Class<? extends View> clazz) {
        return (M) map.get(clazz);
    }
}
