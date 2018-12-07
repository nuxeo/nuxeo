/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Batching utilities.
 *
 * @since 10.10
 */
public class BatchUtils {

    private BatchUtils() {
        // utility class
    }

    /**
     * Takes the input {@code values} and batches them together in groups. All values in a given group have the same
     * derived value. The derived value is computed using the {@code deriver} function. Two derived values are compared
     * for equality using the {@code comparator}.
     *
     * @param <T> the type of the values
     * @param <U> the type of the derived values
     * @param values the input values
     * @param deriver the function to compute a derived value
     * @param comparator the equality test for derived values
     * @return a list of pairs with a derived values and the corresponding batch
     * @since 10.10
     */
    public static <T, U> List<Pair<U, List<T>>> groupByDerived(List<T> values, Function<T, U> deriver,
            BiPredicate<U, U> comparator) {
        List<Pair<U, List<T>>> result = new ArrayList<>();
        U previousDerived = null;
        List<T> batch = null;
        for (T value : values) {
            U derived = deriver.apply(value);
            if (batch == null || !comparator.test(derived, previousDerived)) {
                // start new batch
                batch = new ArrayList<>();
                result.add(Pair.of(derived, batch));
            }
            // add to current batch
            batch.add(value);
            previousDerived = derived;
        }
        return result;
    }

}
