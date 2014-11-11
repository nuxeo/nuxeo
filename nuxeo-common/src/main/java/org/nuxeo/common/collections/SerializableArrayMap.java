/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.common.collections;

import java.io.Serializable;
import java.util.Map;

/**
 * A mixture of an array list and a map used to store small table of elements
 * using both indices and keys.
 * <p>
 * This map accepts null values.
 * <p>
 * The map is implemented using an array of successive [key, value] pairs.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@SuppressWarnings({ "ClassWithoutToString" })
public class SerializableArrayMap<K, V>  implements Serializable {

    private static final long serialVersionUID = 1L;

    // 4 keys, 4 values
    protected static final int DEFAULT_SIZE = 8;

    protected static final int GROW_SIZE = 10;

    protected int count = 0;

    protected Serializable[] elements;

    public SerializableArrayMap() {
    }

    public SerializableArrayMap(int initialCapacity) {
        elements = new Serializable[initialCapacity == 0 ? DEFAULT_SIZE
                : initialCapacity * 2];
    }

    public SerializableArrayMap(Map map) {
        this(map.size());
        putAll(map);
    }

    public SerializableArrayMap(SerializableArrayMap map) {
        count = map.count;
        elements = new Serializable[map.elements.length];
        System.arraycopy(map.elements, 0, elements, 0, count * 2);
    }

    public void putAll(Map<K, V> map) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            Serializable o = (Serializable) entry;
            K key = entry.getKey();
            V value = entry.getValue();
            add(key, value);
        }
    }

    public V remove(K key) {
        if (elements == null || count == 0) {
            return null;
        }
        for (int i = 0; i < elements.length; i += 2) {
            if (elements[i] != null && elements[i].equals(key)) {
                return _remove(i);
            }
        }
        return null;
    }

    public V remove(int index) {
        if (elements == null || count == 0) {
            return null;
        }
        return _remove(index << 1);
    }

    protected final V _remove(int i) {
        V result = (V) elements[i + 1];
        int len = count * 2;
        if (i + 2 == len) {
            elements[i] = null;
            elements[i + 1] = null;
        } else {
            int k = i + 2;
            System.arraycopy(elements, k, elements, i, len - k);
        }
        count--;
        return result;
    }

    public V get(K key) {
        if (elements == null || count == 0) {
            return null;
        }
        for (int i = 0; i < elements.length; i += 2) {
            if (elements[i] != null && elements[i].equals(key)) {
                return (V) elements[i + 1];
            }
        }
        return null;
    }

    public V get(int i) {
        if (elements == null || i >= count) {
            throw new ArrayIndexOutOfBoundsException(i);
        }
        return (V) elements[(i << 1) + 1];
    }

    public K getKey(Serializable value) {
        if (elements == null || count == 0) {
            return null;
        }
        for (int i = 1; i < elements.length; i += 2) {
            if (elements[i] != null && elements[i].equals(value)) {
                return (K) elements[i - 1];
            }
        }
        return null;
    }

    public K getKey(int i) {
        if (elements == null || i >= count) {
            throw new ArrayIndexOutOfBoundsException(i);
        }
        return (K) elements[i << 1];
    }

    public V put(K key, V value) {

        // handle the case where we don't have any attributes yet
        if (elements == null) {
            elements = new Serializable[DEFAULT_SIZE];
        }
        if (count == 0) {
            elements[0] = (Serializable) key;
            elements[1] = (Serializable) value;
            count++;
            return null;
        }

        int insertIndex = count * 2;

        // replace existing value if it exists
        for (int i = 0; i < insertIndex; i += 2) {
            if (elements[i].equals(key)) {
                Serializable oldValue = elements[i + 1];
                elements[i + 1] = (Serializable) value;
                return (V) oldValue;
            }
        }

        if (elements.length <= insertIndex) {
            grow();
        }
        elements[insertIndex] = (Serializable) key;
        elements[insertIndex + 1] = (Serializable) value;
        count++;

        return null;
    }

    public void add(K key, V value) {
        // handle the case where we don't have any attributes yet
        int insertIndex;
        if (elements == null) {
            elements = new Serializable[DEFAULT_SIZE];
            insertIndex = 0;
        } else {
            insertIndex = count * 2;
            if (elements.length <= insertIndex) {
                grow();
            }
        }
        elements[insertIndex] = (Serializable) key;
        elements[insertIndex + 1] = (Serializable) value;
        count++;
    }

    public void trimToSize() {
        int len = count * 2;
        if (len < elements.length) {
            Serializable[] tmp = new Serializable[len];
            System.arraycopy(elements, 0, tmp, 0, len);
            elements = tmp;
        }
    }

    public int size() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public void clear() {
        elements = null;
        count = 0;
    }

    protected void grow() {
        Serializable[] expanded = new Serializable[elements.length + GROW_SIZE];
        System.arraycopy(elements, 0, expanded, 0, elements.length);
        elements = expanded;
    }

    public Serializable[] getArray() {
        return elements;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof SerializableArrayMap) {
            SerializableArrayMap map = (SerializableArrayMap) obj;
            if (count != map.count) {
                return false;
            }
            int len = count << 1;
            for (int i = 0; i < len; i += 2) {
                Serializable key1 = elements[i];
                Serializable key2 = map.elements[i];
                if (!key1.equals(key2)) {
                    return false;
                }
                Serializable val1 = elements[i + 1];
                Serializable val2 = map.elements[i + 1];
                if (val1 == null) {
                    if (val1 != val2) {
                        return false;
                    }
                } else if (!val1.equals(val2)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        for (int i = 0; i < count * 2; i++) {
            result = result * 37 + elements[i].hashCode();
        }
        return result;
    }

}
