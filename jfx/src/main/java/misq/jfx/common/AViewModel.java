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

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import misq.jfx.ApplicationRepo;

import java.util.Optional;

@Slf4j
public abstract class AViewModel implements ViewModel, LifeCycle {
    public interface Listener {
        void onConstructed(ViewModel viewModel);

        void onInitialized();

        void onActivated();

        void onDeactivated();

        void onDestructed();

    }

    public abstract Optional<Listener> getListener();

    public AViewModel() {
        onConstructed(this);
        Platform.runLater(this::onInitialized);
    }

    @Override
    public void onConstructed(ViewModel viewModel) {
        ApplicationRepo.getInstance().onConstructed(this);
        // getListener().ifPresent(listener -> listener.onConstructed(viewModel));
    }

    @Override
    public void onInitialized() {
        ApplicationRepo.getInstance().onInitialized(this);
        //  getListener().ifPresent(Listener::onInitialized);
    }

    @Override
    public void onActivated() {
        getListener().ifPresent(Listener::onActivated);
    }

    @Override
    public void onDeactivated() {
        getListener().ifPresent(Listener::onDeactivated);
    }

    @Override
    public void onDestructed() {
        getListener().ifPresent(Listener::onDestructed);
    }

}
