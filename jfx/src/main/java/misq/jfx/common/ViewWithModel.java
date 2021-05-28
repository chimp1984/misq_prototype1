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

public abstract class ViewWithModel<T extends Node, M extends ViewModel> extends View<T> implements LifeCycle {
    @Getter
    protected final M model;


    public ViewWithModel(T root, M model) {
        super(root);
        this.model = model;

        if (root != null) {
            root.sceneProperty().addListener((ov, oldValue, newValue) -> {
                if (oldValue == null && newValue != null) {
                    model.onViewAdded();
                    onViewAdded();
                } else if (oldValue != null && newValue == null) {
                    model.onViewRemoved();
                    onViewRemoved();
                }
            });
        }
    }

    @Override
    public void onConstructView(ViewModel viewModel) {
    }

    @Override
    public void onViewAdded() {
    }

    @Override
    public void onViewRemoved() {
    }
}
