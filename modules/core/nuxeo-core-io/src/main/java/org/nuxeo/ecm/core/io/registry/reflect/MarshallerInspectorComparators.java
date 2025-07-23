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
package org.nuxeo.ecm.core.io.registry.reflect;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.model.Property;

/**
 * @since 2025.6
 */
public final class MarshallerInspectorComparators {

    // all comparators are reversed because we want higher ranking in the code for understanding but the opposite when
    // manipulating them, ie: a bigger priority marshaller is ranked higher, but we want it to appear first in the
    // MarshallerRegistry collections

    protected static final Comparator<MarshallerInspector> PRIORITY_COMPARATOR = Comparator.comparing(
            MarshallerInspector::getPriority).reversed();

    protected static final Comparator<MarshallerInspector> INSTANTIATIONS_COMPARATOR = Comparator.comparing(
            MarshallerInspector::getInstantiations).reversed();

    protected static final Comparator<MarshallerInspector> OPTIMIZATION_COMPARATOR = new OptimizationComparator().reversed();

    protected static final Comparator<MarshallerInspector> SPECIALIZED_COMPARATOR = new SpecializedComparator().reversed();

    protected static final Comparator<MarshallerInspector> SUB_CLASSES_COMPARATOR = new SubClassComparator().reversed();

    protected static final Comparator<MarshallerInspector> MARSHALLER_CLASS_COMPARATOR = Comparator.<MarshallerInspector, String> comparing(
            i -> i.getMarshallerClass().getName()).reversed();

    public static final Comparator<MarshallerInspector> MARSHALLER_INSPECTOR_COMPARATOR = //
            Comparator.nullsFirst( //
                    PRIORITY_COMPARATOR.thenComparing(INSTANTIATIONS_COMPARATOR)
                                       .thenComparing(OPTIMIZATION_COMPARATOR)
                                       .thenComparing(SPECIALIZED_COMPARATOR)
                                       .thenComparing(SUB_CLASSES_COMPARATOR)
                                       .thenComparing(MARSHALLER_CLASS_COMPARATOR));

    private MarshallerInspectorComparators() {
        // constants class
    }

    /**
     * Comparator used to rank higher some of Nuxeo classes that are used frequently.
     */
    protected static class OptimizationComparator implements Comparator<MarshallerInspector> {

        @Override
        public int compare(MarshallerInspector i1, MarshallerInspector i2) {
            if ((i1.isWriter() && i2.isWriter()) || (i1.isReader() && i2.isReader())) {
                boolean i1IsTop = isTopPriority(i1.getGenericType());
                boolean i2IsTop = isTopPriority(i2.getGenericType());
                if (i1IsTop) {
                    return i2IsTop ? 0 : 1;
                } else if (i2IsTop) {
                    return -1;
                } else {
                    boolean i1IsBig = isBigPriority(i1.getGenericType());
                    boolean i2IsBig = isBigPriority(i2.getGenericType());
                    if (i1IsBig) {
                        return i2IsBig ? 0 : 1;
                    } else if (i2IsBig) {
                        return -1;
                    } else {
                        // both marshallers have not top or big priority - let the next comparator do its computation
                        return 0;
                    }
                }
            }
            // marshallers are not of the same type - let the next comparator do its computation
            return 0;
        }

        protected static boolean isTopPriority(Type type) {
            return TypeUtils.isAssignable(type, DocumentModel.class) || TypeUtils.isAssignable(type, Property.class);
        }

        protected static boolean isBigPriority(Type type) {
            return TypeUtils.isAssignable(type, NuxeoPrincipal.class) || TypeUtils.isAssignable(type, NuxeoGroup.class)
                    || TypeUtils.isAssignable(type, TypeUtils.parameterize(List.class, DocumentModel.class));
        }
    }

    /**
     * Comparator used to sort {@link MarshallerInspector} according to the real entity type they are marshalling, see
     * the list below for supported behavior:
     * <ul>
     * <li>{@code EntityType}</li>
     * <li>{@code Collection<EntityType>}</li>
     * <li>{@code Map<String, EntityType>}</li>
     * </ul>
     * The comparator will sort entities such as:
     * <ul>
     * <li>{@code IntegerProperty > AbstractProperty > Property}</li>
     * <li>{@code Property > Collection<Property>}</li>
     * <li>{@code Collection<Property> > Map<String, Property>}</li>
     * </ul>
     */
    protected static class SpecializedComparator implements Comparator<MarshallerInspector> {

        @Override
        public int compare(MarshallerInspector i1, MarshallerInspector i2) {
            var i1ParameterClass = getContainerParameterClass(i1.getGenericType());
            var i2ParameterClass = getContainerParameterClass(i2.getGenericType());
            var i1FinalMarshalledClass = getIfNull(i1ParameterClass, i1::getMarshalledType);
            var i2FinalMarshalledClass = getIfNull(i2ParameterClass, i2::getMarshalledType);

            // compare directly the final type excluding the container type (ie: Collection/Map)
            if (i1FinalMarshalledClass.equals(i2FinalMarshalledClass)) {
                // final types are equals
                if (i1ParameterClass != null ^ i2ParameterClass != null) {
                    // only one real type is a container
                    // - Property > Collection<Property> (or the opposite)
                    // - Property > Map<String, Property> (or the opposite)
                    return i1ParameterClass == null ? 1 : -1;
                } else if (i1ParameterClass != null) { // && i2ParameterClass != null (always true)
                    // both are containers
                    var i1MarshalledType = i1.getMarshalledType();
                    var i2MarshalledType = i2.getMarshalledType();
                    if ((Collection.class.isAssignableFrom(i1MarshalledType)
                            && Collection.class.isAssignableFrom(i2MarshalledType))
                            || (Map.class.isAssignableFrom(i1MarshalledType)
                                    && Map.class.isAssignableFrom(i2MarshalledType))) {
                        // both marshallers are of the same container type - let the next comparator do its computation
                        return 0;
                    } else {
                        // Collection<Property> > Map<String, Property> (or the opposite)
                        return Collection.class.isAssignableFrom(i1MarshalledType) ? 1 : -1;
                    }
                } else {
                    // both marshallers marshall the same entity - let the next comparator do its computation
                    return 0;
                }
            } else if (i1FinalMarshalledClass.isAssignableFrom(i2FinalMarshalledClass)) {
                // Property < IntegerProperty
                return -1;
            } else if (i2FinalMarshalledClass.isAssignableFrom(i1FinalMarshalledClass)) {
                // Property > IntegerProperty
                return 1;
            } else {
                // real marshalled entities have nothing in common - let the next comparator do its computation
                return 0;
            }
        }

        protected static Class<?> getContainerParameterClass(Type type) {
            Map<TypeVariable<?>, Type> typeArguments;
            // first try Collection
            typeArguments = TypeUtils.getTypeArguments(type, Collection.class);
            if (typeArguments != null) {
                var parameterType = typeArguments.get(Collection.class.getTypeParameters()[0]);
                return TypeUtils.getRawType(parameterType, null);
            }
            // then try Map
            typeArguments = TypeUtils.getTypeArguments(type, Map.class);
            if (typeArguments != null) {
                var parameterType = typeArguments.get(Map.class.getTypeParameters()[1]);
                return TypeUtils.getRawType(parameterType, null);
            }
            // finally give up
            return null;
        }
    }

    /**
     * Force subclasses to manage their priorities: {@code StandardWriter > CustomWriter extends StandardWriter}.
     * <p>
     * Let the reference implementations priority.
     */
    protected static class SubClassComparator implements Comparator<MarshallerInspector> {

        @Override
        public int compare(MarshallerInspector i1, MarshallerInspector i2) {
            if (i1.getMarshallerClass().equals(i2.getMarshallerClass())) {
                return 0;
            } else if (i1.getMarshallerClass().isAssignableFrom(i2.getMarshallerClass())) {
                // StandardWriter > CustomWriter
                return 1;
            } else if (i2.getMarshallerClass().isAssignableFrom(i1.getMarshallerClass())) {
                // CustomWriter < StandardWriter
                return -1;
            } else {
                // none of them extends the other - let the next comparator do its computation
                return 0;
            }
        }
    }
}
