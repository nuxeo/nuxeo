/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
     * Returns a batched version of the list, according to the passed parameters.
     *
     * @param list the list
     * @param maxItems the maximum number of items, or {@code null} for the default
     * @param skipCount the skip count
     * @param defaultMax the default maximum number of items if {@code maxItems} is {@code null}
     * @return the batched list, which may be a sublist per {@link List#subList}
     */
    public static <T> List<T> batchList(List<T> list, BigInteger maxItems, BigInteger skipCount, int defaultMax) {
        return getBatchedList(list, maxItems, skipCount, defaultMax).getList();
    }

    /**
     * Returns a batched version of the list, according to the passed parameters.
     *
     * @param list the list
     * @param maxItems the maximum number of items, or {@code null} for the default
     * @param skipCount the skip count
     * @param defaultMax the default maximum number of items if {@code maxItems} is {@code null}
     * @return the batched list, which may be a sublist per {@link List#subList}
     */
    public static <T> BatchedList<T> getBatchedList(List<T> list, BigInteger maxItems, BigInteger skipCount,
            int defaultMax) {
        int skip = skipCount == null ? 0 : skipCount.intValue();
        if (skip < 0) {
            skip = 0;
        }
        int max = maxItems == null ? -1 : maxItems.intValue();
        if (max < 0) {
            max = defaultMax;
        }
        BatchedList<T> res = new BatchedList<>();
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
     * A holder for a sublist of a list, a flag indicating if there were more elements after the included sublist, and
     * the total number of items if there had been no batching.
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
