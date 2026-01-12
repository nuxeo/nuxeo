/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.common.utils;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * @since 2025.14
 */
public final class ReflectUtils {

    private ReflectUtils() {
        // utility class
    }

    /**
     * Returns the given list typed with a parent type. The benefit of this method is that it checks that the {@link O}
     * type is assignable to {@link B} type at compile time.
     * <p>
     * For example when you want a List&lt;Number&gt; from a List&lt;Long&gt;.
     * <pre>
     * {@code
     * List<Long> longList = List.of(1L, 2L, 3L);
     * 
     * // casting List<Long> to List<Number> without the helper
     * List<number> previousNumberList = (List<Number>) ((List<?>) longList);
     * 
     * // casting List<Long> to List<Number> with the helper
     * List<Number> numberList = ReflectUtils.downgradeCast(longList);
     * }
     * </pre>
     *
     * @param originalList the list to downgrade cast
     * @param <O> the original list type
     * @param <B> the target parent list type
     * @return the given list typed with a parent type
     */
    @SuppressWarnings("unchecked")
    public static <O extends B, B> List<B> downgradeCast(List<O> originalList) {
        return (List<B>) originalList;
    }

    /**
     * Retrieves the first generic parameter type {@code T} from a class {@code O} that extends a typed class
     * {@code D<T, ...>}.
     *
     * @param clazz the instance to extract the target type from
     * @param typedClass the type that declares the generic parameter
     * @return the type from the first generic parameter
     * @param <T> the target type to retrieve
     * @param <D> the type that declares the generic parameter, such as {@code D<T, ...>}
     * @param <O> the type that infers the generic parameter, such as {@code O extends D<T, ...>}
     */
    @SuppressWarnings("unchecked")
    public static <T, D, O extends D> Class<T> retrieveFirstParameterType(Class<O> clazz, Class<D> typedClass) {
        Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(clazz, typedClass);
        for (Map.Entry<TypeVariable<?>, Type> entry : typeArguments.entrySet()) {
            if (typedClass.equals(entry.getKey().getGenericDeclaration())) {
                var writerType = TypeUtils.unrollVariables(typeArguments, entry.getValue());
                return (Class<T>) TypeUtils.getRawType(writerType, null);
            }
        }
        throw new IllegalArgumentException("Unable to retrieve the type: T from %s<T> for class: %s".formatted(
                typedClass.getSimpleName(), clazz.getName()));
    }

    /**
     * Retrieves a generic parameter type {@code T} from a class {@code O} that extends a typed class
     * {@code D<..., T, ...>}, by its parameter name.
     *
     * @param clazz the instance to extract the target type from
     * @param typedClass the type that declares the generic parameter
     * @return the parameterName types from the generic parameters
     * @param <D> the type that declares the generic parameter, such as {@code D<..., T, ..>}
     * @param <O> the type that infers the generic parameter, such as {@code O extends D<..., T, ...>}
     */
    public static <D, O extends D> Class<?>[] retrieveParameterTypes(Class<O> clazz, Class<D> typedClass,
            String parameterName, String... parameterNames) {
        String[] names = ArrayUtils.addFirst(parameterNames, parameterName);
        Class<?>[] types = new Class[names.length];
        Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(clazz, typedClass);
        for (Map.Entry<TypeVariable<?>, Type> entry : typeArguments.entrySet()) {
            if (typedClass.equals(entry.getKey().getGenericDeclaration())) {
                int nameIdx = ArrayUtils.indexOf(names, entry.getKey().getName());
                if (nameIdx != ArrayUtils.INDEX_NOT_FOUND) {
                    var type = TypeUtils.unrollVariables(typeArguments, entry.getValue());
                    types[nameIdx] = TypeUtils.getRawType(type, null);
                }
            }
        }
        var exception = new IllegalArgumentException(
                "Unable to retrieve one of the requested types: %s from %s<T> for class: %s".formatted(names,
                        typedClass.getSimpleName(), clazz.getName()));
        for (int i = 0; i < names.length; i++) {
            var type = types[i];
            if (type == null) {
                exception.addSuppressed(new IllegalArgumentException(
                        "Unable to retrieve the type: %s from %s".formatted(names[i], typedClass.getSimpleName())));
            }
        }
        if (exception.getSuppressed().length > 0) {
            throw exception;
        }
        return types;
    }
}
