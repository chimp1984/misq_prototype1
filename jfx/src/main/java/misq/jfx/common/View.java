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

package misq.jfx.common;

import javafx.scene.Node;
import lombok.Getter;

public abstract class View<T extends Node> {
    @Getter
    protected final T root;

    public View(T root) {
        this.root = root;
        if (root != null) {
            root.sceneProperty().addListener((ov, oldValue, newValue) -> {
                if (oldValue == null && newValue != null) {
                    onAddedToStage();
                } else if (oldValue != null && newValue == null) {
                    onRemovedFromStage();
                }
            });
        }
    }

    protected void onAddedToStage() {
    }

    protected void onRemovedFromStage() {
    }

    protected void setupView() {
    }

    protected void configModel() {
    }

    protected void configController() {
    }
}
