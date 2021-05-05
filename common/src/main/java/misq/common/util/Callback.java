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

package misq.common.util;

import javax.annotation.Nullable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Call back handler for supporting optional mapping to executor thread if executor is provided.
 * If no result is required R should be set to Void and the onResult method without parameters is used.
 * The exceptionHandler is optional as well.
 *
 * @param <R> The type of the result. Void if no result is expected.
 */
public class Callback<R> {
    private final Consumer<R> resultHandler;
    @Nullable
    private final Consumer<Exception> exceptionHandler;
    @Nullable
    private final Executor executor;

    public Callback(Runnable resultHandler) {
        this(r -> resultHandler.run(), null, null);
    }

    public Callback(Runnable resultHandler, Consumer<Exception> exceptionHandler) {
        this(r -> resultHandler.run(), exceptionHandler, null);
    }

    public Callback(Runnable resultHandler, Consumer<Exception> exceptionHandler, Executor executor) {
        this(r -> resultHandler.run(), exceptionHandler, executor);
    }

    public Callback(Consumer<R> resultHandler) {
        this(resultHandler, null, null);
    }

    public Callback(Consumer<R> resultHandler, Consumer<Exception> exceptionHandler) {
        this(resultHandler, exceptionHandler, null);
    }

    public Callback(Consumer<R> resultHandler, @Nullable Consumer<Exception> exceptionHandler, @Nullable Executor executor) {
        this.resultHandler = resultHandler;
        this.exceptionHandler = exceptionHandler;
        this.executor = executor;
    }

    public void onResult(R result) {
        if (executor != null) {
            executor.execute(() -> resultHandler.accept(result));
        } else {
            resultHandler.accept(result);
        }
    }

    public void onResult() {
        if (executor != null) {
            executor.execute(() -> resultHandler.accept(null));
        } else {
            resultHandler.accept(null);
        }
    }

    public void onFault(Exception exception) {
        if (exceptionHandler == null) {
            return;
        }
        if (executor != null) {
            executor.execute(() -> exceptionHandler.accept(exception));
        } else {
            exceptionHandler.accept(exception);
        }
    }
}
