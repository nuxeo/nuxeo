/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.nuxeo.common.utils.BatchUtils;

public class TestBatchUtils {

    @Test
    public void testGroupByDerived() {
        Function<String, String> deriver = s -> s.substring(0, 1); // first letter
        BiPredicate<String, String> comparator = (a, b) -> a.equals(b);
        List<String> values;
        List<Pair<String, List<String>>> result;

        values = asList();
        result = BatchUtils.groupByDerived(values, deriver, comparator);
        assertEquals("[]", result.toString());

        values = asList("a1");
        result = BatchUtils.groupByDerived(values, deriver, comparator);
        assertEquals("[(a,[a1])]", result.toString());

        values = asList("a1", "a2", "a3");
        result = BatchUtils.groupByDerived(values, deriver, comparator);
        assertEquals("[(a,[a1, a2, a3])]", result.toString());

        values = asList("a1", "a2", "b3");
        result = BatchUtils.groupByDerived(values, deriver, comparator);
        assertEquals("[(a,[a1, a2]), (b,[b3])]", result.toString());

        values = asList("a1", "b2", "a3", "b4");
        result = BatchUtils.groupByDerived(values, deriver, comparator);
        assertEquals("[(a,[a1]), (b,[b2]), (a,[a3]), (b,[b4])]", result.toString());

        values = asList("a1", "a2", "b3", "c4", "a5", "a6", "a7", "c8");
        result = BatchUtils.groupByDerived(values, deriver, comparator);
        assertEquals("[(a,[a1, a2]), (b,[b3]), (c,[c4]), (a,[a5, a6, a7]), (c,[c8])]", result.toString());
    }

}
