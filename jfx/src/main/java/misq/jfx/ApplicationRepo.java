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

import misq.jfx.common.AViewModel;
import misq.jfx.common.ViewModel;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class ApplicationRepo {
    private static ApplicationRepo INSTANCE;
    public final Thread javaFXApplicationThread;
    private final Map<Class<? extends ViewModel>, Set<AViewModel.Listener>> lifeCycleListeners = new ConcurrentHashMap<>();


    public ApplicationRepo(Thread javaFXApplicationThread) {
        INSTANCE = this;
        this.javaFXApplicationThread = javaFXApplicationThread;
    }

    public static ApplicationRepo getInstance() {
        return INSTANCE;
    }

    public void addLifeCycleListener(Class<? extends ViewModel> clazz, AViewModel.Listener listener) {
        if (!lifeCycleListeners.containsKey(clazz)) {
            lifeCycleListeners.put(clazz, new CopyOnWriteArraySet<>());
        }
        lifeCycleListeners.get(clazz).add(listener);
    }

    public void onConstructed(AViewModel viewModel) {
        if (lifeCycleListeners.containsKey(viewModel.getClass())) {
            lifeCycleListeners.get(viewModel.getClass()).forEach(e -> e.onConstructed(viewModel));
        }
    }

    public void onInitialized(AViewModel viewModel) {
        if (lifeCycleListeners.containsKey(viewModel.getClass())) {
            lifeCycleListeners.get(viewModel.getClass()).forEach(AViewModel.Listener::onInitialized);
        }
    }
}
