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
import misq.jfx.MvcInjector;
import misq.presentation.Controller;
import misq.presentation.Model;

public abstract class ViewWithModelAndController<T extends Node, C extends Controller, M extends Model> extends View<T> {
    protected C controller;
    protected final M model;

    public ViewWithModelAndController(T root, Class<C> controllerClass) {
        super(root);

        model = MvcInjector.getModel(this.getClass());
        try {
            controller = controllerClass.getDeclaredConstructor(model.getClass()).newInstance(model);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (root != null) {
            root.sceneProperty().addListener((ov, oldValue, newValue) -> {
                if (oldValue == null && newValue != null) {
                    onViewAdded();
                } else if (oldValue != null && newValue == null) {
                    onViewRemoved();
                }
            });
        }
    }

    public void onViewAdded() {
    }

    public void onViewRemoved() {
    }
}
