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

package misq.common.persistence;

import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class Persistence {
    public static void write(Persistable persistable) {
        write(persistable, persistable.getDefaultStorageFileName());
    }

    public static void write(Serializable serializable, String fileName) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(fileName);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
            objectOutputStream.writeObject(serializable);
            objectOutputStream.flush();
            fileOutputStream.flush();
            fileOutputStream.getFD().sync();
        } catch (IOException exception) {
            log.error(exception.toString(), exception);
        }
    }

    public static Serializable read(String fileName) {
        try (FileInputStream fileInputStream = new FileInputStream(fileName);
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
            return (Serializable) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException exception) {
            log.error(exception.toString(), exception);
            return null;
        }
    }
}
