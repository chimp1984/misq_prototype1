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

package misq.jfx.navigation;


import javafx.scene.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class ViewPath extends ArrayList<Class<? extends Node>> {
    private ViewPath() {
    }

    public ViewPath(Collection<? extends Class<? extends Node>> c) {
        super(c);
    }

    @SafeVarargs
    public static ViewPath to(Class<? extends Node>... elements) {
        ViewPath path = new ViewPath();
        List<Class<? extends Node>> list = Arrays.asList(elements);
        path.addAll(list);
        return path;
    }

    public static ViewPath from(ViewPath original) {
        ViewPath path = new ViewPath();
        path.addAll(original);
        return path;
    }

    public Class<? extends Node> tip() {
        if (size() == 0)
            return null;

        return get(size() - 1);
    }
}
