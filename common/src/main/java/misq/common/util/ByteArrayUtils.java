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

import java.nio.ByteBuffer;

public class ByteArrayUtils {
  /*  public static byte[] concat(byte[] first, byte[] second) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(first.length + second.length);
        byteBuffer.put(first);
        byteBuffer.put(second);
        return byteBuffer.array();
    }

    public static byte[] concat(byte[] first, byte[] second, byte[] third) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(first.length + second.length + third.length);
        byteBuffer.put(first);
        byteBuffer.put(second);
        byteBuffer.put(third);
        return byteBuffer.array();
    }

    public static byte[] concat(byte[] first, byte[] second, byte[] third, byte[] forth) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(first.length + second.length + third.length + forth.length);
        byteBuffer.put(first);
        byteBuffer.put(second);
        byteBuffer.put(third);
        byteBuffer.put(forth);
        return byteBuffer.array();
    }

    public static byte[] concat(byte[] first, byte[] second, byte[] third, byte[] forth, byte[] fifth) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(first.length + second.length + third.length + forth.length + fifth.length);
        byteBuffer.put(first);
        byteBuffer.put(second);
        byteBuffer.put(third);
        byteBuffer.put(forth);
        byteBuffer.put(fifth);
        return byteBuffer.array();
    }
*/
  public static byte[] concat(byte[]... bytes) {
      int length = 0;
      for (byte[] aByte : bytes) {
          length += aByte.length;
      }
      ByteBuffer byteBuffer = ByteBuffer.allocate(length);
      for (byte[] aByte : bytes) {
          byteBuffer.put(aByte);
      }
      return byteBuffer.array();
  }
}
