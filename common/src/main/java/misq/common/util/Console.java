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

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
public class Console {
    private static final Set<Consumer<String>> listeners = new HashSet<>();
    private static Scanner scanner;
    private static Thread thread;

    public static void init() {
        init(System.in);
    }

    public static void init(InputStream inputStream) {
        if (thread != null) {
            return;
        }

        thread = new Thread(() -> {
            scanner = new Scanner(inputStream);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                listeners.forEach(l -> l.accept(line));
            }
        });
        thread.start();
    }

    public static void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }
}
