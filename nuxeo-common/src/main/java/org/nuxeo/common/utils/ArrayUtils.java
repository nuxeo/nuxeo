/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: ArrayUtils.java 28607 2008-01-09 15:49:32Z sfermigier $
 */

package org.nuxeo.common.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Array utils.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@SuppressWarnings({"SuppressionAnnotation"})
public final class ArrayUtils {

    // This is an utility class.
    private ArrayUtils() {
    }

    /**
     * Merges any number of Array.
     * <p>
     * Comes from :
     * http://forum.java.sun.com/thread.jspa?threadID=202127&messageID=676603
     *
     * @param arrays several arrays
     * @return a merged array
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] arrayMerge(T[]... arrays) {
        int count = 0;
        Class<?> klass = null;
        for (T[] array : arrays) {
            count += array.length;
            if (klass == null && array.length > 0) {
                klass = array[0].getClass();
            }
        }
        if (count == 0) {
            // all arrays are empty, return the first one
            return arrays[0];
        }
        // create new array
        T[] rv = (T[]) Array.newInstance(klass, count);
        int start = 0;
        for (T[] array : arrays) {
            System.arraycopy(array, 0, rv, start, array.length);
            start += array.length;
        }
        return rv;
    }

    /**
     * Method for intersecting arrays elements. Copy of the first array and
     * remove progressively elements if not found in the other arrays.
     * <p>
     * This method will keep the initial order of elements (as found in the
     * first array).
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] intersect(final T[]... arrays) {
        final Class type = arrays.getClass().getComponentType()
                .getComponentType();
        if (arrays.length == 0) {
            return (T[]) Array.newInstance(type, 0);
        }

        final List<T> commonItems = new ArrayList<T>();

        final T[] firstArray = arrays[0];
        commonItems.addAll(Arrays.asList(firstArray));

        // check with the other arrays
        // we skip the first array
        for (int i = 1; i < arrays.length; i++) {
            final T[] array = arrays[i];

            final List<T> arrayAsList = Arrays.asList(array);

            final Set<T> itemsToRemove = new HashSet<T>();
            for (T item : commonItems) {
                if (!arrayAsList.contains(item)) {
                    itemsToRemove.add(item);
                }
            }

            commonItems.removeAll(itemsToRemove);
        }

        T[] result = (T[]) Array.newInstance(type, commonItems.size());
        result = commonItems.toArray(result);

        return result;
    }

}
