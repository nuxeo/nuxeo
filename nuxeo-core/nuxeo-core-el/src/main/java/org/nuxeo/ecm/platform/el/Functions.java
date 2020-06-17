/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.el;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Helper functions injected in the {@link ExpressionContext} instance.
 *
 * @since 11.2
 */
public class Functions {

    /**
     * Returns {@code true} if the given {@code arr} contains the given {@code element}.
     * <p>
     * Always returns {@code false} if the given {@code arr} is {@code null}.
     */
    public static boolean arrayContains(Object[] arr, Object element) {
        if (arr == null) {
            return false;
        }
        return Arrays.asList(arr).contains(element);
    }

    /**
     * Returns {@code true} if the given {@code arr} contains all the given {@code elements}.
     * <p>
     * Always returns {@code false} if the given {@code arr} is {@code null}.
     */
    public static boolean arrayContainsAll(Object[] arr, Object... elements) {
        if (arr == null || elements == null) {
            return false;
        }
        return Arrays.asList(arr).containsAll(Arrays.asList(elements));
    }

    /**
     * Returns {@code true} if the given {@code arr} contains one of the given {@code elements}.
     * <p>
     * Always returns {@code false} if the given {@code arr} is {@code null}.
     */
    public static boolean arrayContainsAny(Object[] arr, Object... elements) {
        if (arr == null || elements == null) {
            return false;
        }
        List<Object> list = Arrays.asList(arr);
        return Stream.of(elements).anyMatch(list::contains);
    }

    /**
     * Returns {@code true} if the given {@code arr} contains none of the given {@code elements}.
     * <p>
     * Always returns {@code false} if the given {@code arr} is {@code null}.
     */
    public static boolean arrayContainsNone(Object[] arr, Object... elements) {
        if (arr == null || elements == null) {
            return false;
        }
        List<Object> list = Arrays.asList(arr);
        return Stream.of(elements).noneMatch(list::contains);
    }

    private Functions() {
        // helper class
    }
}
