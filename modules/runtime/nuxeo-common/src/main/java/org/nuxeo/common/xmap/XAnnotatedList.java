/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Anahide Tchertchian
 */

package org.nuxeo.common.xmap;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.nuxeo.common.collections.PrimitiveArrays;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.registry.XMerge;
import org.nuxeo.common.xmap.registry.XRemove;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Processor for annotated field or method into a list-like object.
 */
public class XAnnotatedList<T, V> extends XAnnotatedMember<T> {

    protected final ElementVisitor<V> elementListVisitor = new ElementVisitor<>();

    protected final ElementValueVisitor<V> elementVisitor = new ElementValueVisitor<>();

    protected final AttributeValueVisitor<V> attributeVisitor = new AttributeValueVisitor<>();

    // indicates the type of the collection components
    protected Class<V> componentType;

    protected XAnnotatedObject<V> componentXao;

    protected XValueFactory<V> componentValueFactory;

    protected boolean isNullByDefault;

    protected XAnnotatedReference<Boolean> merge;

    protected XAnnotatedReference<Boolean> remove;

    protected XAnnotatedList(XMap xmap, XAccessor<T> setter) {
        super(xmap, setter);
    }

    public XAnnotatedList(XMap xmap, XAccessor<T> setter, XNodeList anno) {
        super(xmap, setter);
        path = new Path(anno.value());
        trim = anno.trim();
        type = (Class<T>) anno.type();
        componentType = (Class<V>) anno.componentType();
        componentValueFactory = xmap.getValueFactory(componentType);
        componentXao = xmap.register(componentType);
        isNullByDefault = anno.nullByDefault();
    }

    /**
     * Sets the {@link XMerge} annotation that was resolved for this list.
     *
     * @since 11.5
     */
    public void setMerge(XAnnotatedReference<Boolean> merge) {
        this.merge = merge;
    }

    /**
     * Sets the {@link XRemove} annotation that was resolved for this list.
     *
     * @since 11.5
     */
    public void setRemove(XAnnotatedReference<Boolean> remove) {
        this.remove = remove;
    }

    @Override
    public void process(Context ctx, Element element, Object existing) {
        if (remove != null && Boolean.TRUE.equals(remove.getValue(ctx, element))) {
            setValue(ctx.getObject(), convertList(Collections.emptyList()));
            return;
        }
        List<V> initList = new ArrayList<>();
        if (existing != null
                && (!hasValue(ctx, element) || merge == null || Boolean.TRUE.equals(merge.getValue(ctx, element)))) {
            V[] currentListValue = getCurrentListValue(existing);
            if (currentListValue != null) {
                initList.addAll(Arrays.asList(currentListValue));
            }
        }
        T value = getValue(ctx, element, initList);
        if (value != null) {
            setValue(ctx.getObject(), value);
        }
    }

    @Override
    public T getValue(Context ctx, Element base) {
        return getValue(ctx, base, new ArrayList<>());
    }

    protected T getValue(Context ctx, Element base, List<V> values) {
        if (componentXao != null) {
            DOMHelper.visitNodes(ctx, this, base, path, elementListVisitor, values);
        } else {
            if (path.attribute != null) {
                // attribute list
                DOMHelper.visitNodes(ctx, this, base, path, attributeVisitor, values);
            } else {
                // element list
                DOMHelper.visitNodes(ctx, this, base, path, elementVisitor, values);
            }
        }
        return convertList(values);
    }

    protected T convertList(List<V> values) {
        if (isNullByDefault && values.isEmpty()) {
            return null;
        }

        if (type != ArrayList.class) {
            if (type.isArray()) {
                if (componentType.isPrimitive()) {
                    // primitive arrays cannot be casted to Object[]
                    return (T) PrimitiveArrays.toPrimitiveArray((Collection<Object>) values, (Class<?>) componentType);
                } else {
                    return (T) values.toArray((V[]) Array.newInstance(componentType, values.size()));
                }
            } else {
                try {
                    T col = type.getDeclaredConstructor().newInstance();
                    ((Collection<V>) col).addAll(values);
                    return col;
                } catch (ReflectiveOperationException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }

        return (T) values;
    }

    protected V[] getCurrentListValue(Object instance) {
        Object v = accessor.getValue(instance);
        if (v != null) {
            Object[] objects;
            if (v instanceof Object[]) {
                objects = (Object[]) v;
            } else if (v instanceof List) {
                objects = ((List<?>) v).toArray();
            } else if (v instanceof Collection) {
                objects = ((Collection<?>) v).toArray();
            } else {
                objects = PrimitiveArrays.toObjectArray(v);
            }
            return (V[]) objects;
        }
        return null;
    }

    @Override
    public void toXML(Object instance, Element parent) {
        V[] objects = getCurrentListValue(instance);
        if (objects != null) {
            if (componentXao == null) {
                for (V o : objects) {
                    if (componentValueFactory != null && !(o instanceof Element)) {
                        String value = componentValueFactory.serialize(null, o);
                        if (value != null) {
                            Element e = XMLBuilder.addElement(parent, path);
                            XMLBuilder.fillField(e, value, path.attribute);
                        }
                    }
                }
            } else {
                for (Object o : objects) {
                    Element e = XMLBuilder.addElement(parent, path);
                    XMLBuilder.toXML(o, e, componentXao);
                }
            }
        }
    }
}

class ElementVisitor<T> implements DOMHelper.NodeVisitor<T> {

    @Override
    public void visitNode(Context ctx, XAnnotatedMember<T> xam, Node node, Collection<T> result) {
        result.add(xam.xao.newInstance(ctx, (Element) node));
    }

}

class ElementValueVisitor<T> implements DOMHelper.NodeVisitor<T> {
    @Override
    public void visitNode(Context ctx, XAnnotatedMember<T> xam, Node node, Collection<T> result) {
        if (((XAnnotatedList<?, ?>) xam).componentType == Element.class) {
            result.add((T) node);
            return;
        }
        String val = node.getTextContent();
        if (xam.trim) {
            val = val.trim();
        }
        if (xam.valueFactory != null) {
            result.add(xam.valueFactory.deserialize(ctx, val));
        } else {
            // TODO: log warning?
            result.add((T) val);
        }
    }
}

class AttributeValueVisitor<T> implements DOMHelper.NodeVisitor<T> {
    @Override
    public void visitNode(Context ctx, XAnnotatedMember<T> xam, Node node, Collection<T> result) {
        String val = node.getNodeValue();
        if (xam.valueFactory != null) {
            result.add(xam.valueFactory.deserialize(ctx, val));
        } else {
            // TODO: log warning?
            result.add((T) val);
        }
    }
}
