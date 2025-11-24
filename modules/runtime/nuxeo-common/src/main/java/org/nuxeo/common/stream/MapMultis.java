/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.common.stream;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @since 2025.12
 */
public class MapMultis {

    private MapMultis() {
        // utility class
    }

    /**
     * Returns a {@link BiConsumer} to use with {@link java.util.stream.Stream#mapMulti(BiConsumer)} in order to get a
     * stream on a {@link Collection}. This is an equivalent of the flatMap approach:
     * 
     * <pre>
     * {@code
     * users.stream().map(User::getGroups).flatMap(List::stream);
     * users.stream().mapMulti(MapMultis.each(User::getGroups));
     * }
     * </pre>
     *
     * @param mapper the mapping function to produce elements
     * @return a {@link BiConsumer} to use with mapMulti
     * @param <T> the type of elements within the stream
     * @param <R> the type of elements within the collection
     */
    public static <T, R> BiConsumer<? super T, Consumer<R>> each(Function<T, ? extends Collection<R>> mapper) {
        return (elements, downstream) -> {
            for (var element : mapper.apply(elements)) {
                downstream.accept(element);
            }
        };
    }

    /**
     * Returns a {@link BiConsumer} to use with {@link java.util.stream.Stream#mapMulti(BiConsumer)} in order to get a
     * stream on a {@link Collection} that passes the given {@link Predicate}. This is an equivalent of the
     * flatMap/filter approach:
     *
     * <pre>
     * {@code
     * users.stream().map(User::getGroups).flatMap(List::stream).filter(Objects::nonNull);
     * users.stream().mapMulti(MapMultis.eachIf(User::getGroups, Objects::nonNull));
     * }
     * </pre>
     *
     * @param mapper the mapping function to produce elements
     * @param predicate the predicate that produced elements must pass
     * @return a {@link BiConsumer} to use with mapMulti
     * @param <T> the type of elements within the stream
     * @param <R> the type of elements within the collection
     */
    public static <T, R> BiConsumer<? super T, Consumer<R>> eachIf(Function<T, ? extends Collection<R>> mapper,
            Predicate<R> predicate) {
        return (elements, downstream) -> {
            for (var element : mapper.apply(elements)) {
                if (predicate.test(element)) {
                    downstream.accept(element);
                }
            }
        };
    }
}
