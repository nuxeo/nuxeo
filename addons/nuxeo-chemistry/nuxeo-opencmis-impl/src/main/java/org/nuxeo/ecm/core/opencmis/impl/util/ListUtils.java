/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.util;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

/**
 * Utilities around lists.
 */
public class ListUtils {

    private ListUtils() {
        // utility class
    }

    /**
     * Returns a batched version of the list, according to the passed
     * parameters.
     *
     * @param list the list
     * @param maxItems the maximum number of items, or {@code null} for the
     *            default
     * @param skipCount the skip count
     * @param defaultMax the default maximum number of items if {@code maxItems}
     *            is {@code null}
     * @return the batched list, which may be a sublist per {@link List#subList}
     */
    public static <T> List<T> batchList(List<T> list, BigInteger maxItems,
            BigInteger skipCount, int defaultMax) {
        return getBatchedList(list, maxItems, skipCount, defaultMax).getList();
    }

    /**
     * Returns a batched version of the list, according to the passed
     * parameters.
     *
     * @param list the list
     * @param maxItems the maximum number of items, or {@code null} for the
     *            default
     * @param skipCount the skip count
     * @param defaultMax the default maximum number of items if {@code maxItems}
     *            is {@code null}
     * @return the batched list, which may be a sublist per {@link List#subList}
     */
    public static <T> BatchedList<T> getBatchedList(List<T> list,
            BigInteger maxItems, BigInteger skipCount, int defaultMax) {
        int skip = skipCount == null ? 0 : skipCount.intValue();
        if (skip < 0) {
            skip = 0;
        }
        int max = maxItems == null ? -1 : maxItems.intValue();
        if (max < 0) {
            max = defaultMax;
        }
        BatchedList<T> res = new BatchedList<T>();
        res.setNumItems(list.size());
        if (skip >= list.size()) {
            res.setHasMoreItems(false);
            res.setList(Collections.<T> emptyList());
            return res;
        }
        if (max > list.size() - skip) {
            max = list.size() - skip;
        }
        boolean hasMoreItems = max < list.size() - skip;
        if (skip > 0 || hasMoreItems) {
            list = list.subList(skip, skip + max);
        }
        res.setHasMoreItems(hasMoreItems);
        res.setList(list);
        return res;
    }

    /**
     * A holder for a sublist of a list, a flag indicating if there were more
     * elements after the included sublist, and the total number of items if
     * there had been no batching.
     *
     * @param <T> the type of the list elements
     */
    public static class BatchedList<T> {

        public List<T> list;

        public boolean hasMoreItems = false;

        public int numItems;

        public List<T> getList() {
            return list;
        }

        public void setList(List<T> list) {
            this.list = list;
        }

        public Boolean getHasMoreItems() {
            return Boolean.valueOf(hasMoreItems);
        }

        public void setHasMoreItems(boolean hasMoreItems) {
            this.hasMoreItems = hasMoreItems;
        }

        public BigInteger getNumItems() {
            return BigInteger.valueOf(numItems);
        }

        public void setNumItems(int numItems) {
            this.numItems = numItems;
        }
    }

}
