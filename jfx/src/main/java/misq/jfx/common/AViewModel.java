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

import lombok.extern.slf4j.Slf4j;
import misq.jfx.ApplicationModel;

@Slf4j
public abstract class AViewModel implements ViewModel {
    public AViewModel() {
        onConstructView(this);
    }

    @Override
    public void onConstructView(ViewModel viewModel) {
        ApplicationModel applicationModel = ApplicationModel.getInstance();
        if (applicationModel != null) {
            applicationModel.onConstructView(this);
        }
    }

    @Override
    public void onViewAdded() {
        ApplicationModel applicationModel = ApplicationModel.getInstance();
        if (applicationModel != null) {
            applicationModel.onViewAdded(this.getClass());
        }
    }

    @Override
    public void onViewRemoved() {
        ApplicationModel applicationModel = ApplicationModel.getInstance();
        if (applicationModel != null) {
            applicationModel.onViewRemoved(this.getClass());
        }
    }
}