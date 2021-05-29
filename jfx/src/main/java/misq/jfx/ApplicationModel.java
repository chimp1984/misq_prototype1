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
import misq.jfx.common.AViewModel;
import misq.jfx.common.LifeCycleChangeListener;
import misq.jfx.common.ViewModel;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class ApplicationModel {
    private static ApplicationModel INSTANCE;
    public final Thread javaFXApplicationThread;


    private final Map<Class<? extends ViewModel>, Set<LifeCycleChangeListener>> lifeCycleListeners = new ConcurrentHashMap<>();


    public ApplicationModel(Thread javaFXApplicationThread) {
        INSTANCE = this;
        this.javaFXApplicationThread = javaFXApplicationThread;
    }

    public static ApplicationModel getInstance() {
        return INSTANCE;
    }

    public void connect(Class<? extends ViewModel> clazz, LifeCycleChangeListener lifeCycleChangeListener) {
        if (!lifeCycleListeners.containsKey(clazz)) {
            lifeCycleListeners.put(clazz, new CopyOnWriteArraySet<>());
        }
        lifeCycleListeners.get(clazz).add(lifeCycleChangeListener);
    }

    public void onConstructView(ViewModel viewModel) {
        if (lifeCycleListeners.containsKey(viewModel.getClass())) {
            lifeCycleListeners.get(viewModel.getClass()).forEach(listener -> {
                listener.onConstructView(viewModel);
            });
        }
    }

    public void onViewAdded(Class<? extends AViewModel> clazz) {
        if (lifeCycleListeners.containsKey(clazz)) {
            lifeCycleListeners.get(clazz).forEach(LifeCycleChangeListener::onViewAdded);
        }
    }

    public void onViewRemoved(Class<? extends AViewModel> clazz) {
        if (lifeCycleListeners.containsKey(clazz)) {
            lifeCycleListeners.get(clazz).forEach(LifeCycleChangeListener::onViewRemoved);
        }
    }
}
