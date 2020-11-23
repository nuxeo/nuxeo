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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class XAnnotatedList extends XAnnotatedMember {

    protected static final ElementVisitor elementListVisitor = new ElementVisitor();

    protected static final ElementValueVisitor elementVisitor = new ElementValueVisitor();

    protected static final AttributeValueVisitor attributeVisitor = new AttributeValueVisitor();

    // indicates the type of the collection components
    protected Class<?> componentType;

    protected boolean isNullByDefault;

    protected XAnnotatedReference merge;

    protected XAnnotatedReference remove;

    protected XAnnotatedList(XMap xmap, XAccessor setter) {
        super(xmap, setter);
    }

    public XAnnotatedList(XMap xmap, XAccessor setter, XNodeList anno) {
        super(xmap, setter);
        path = new Path(anno.value());
        trim = anno.trim();
        type = anno.type();
        componentType = anno.componentType();
        valueFactory = xmap.getValueFactory(componentType);
        xao = xmap.register(componentType);
        isNullByDefault = anno.nullByDefault();
    }

    /**
     * @since TODO
     */
    public void setMerge(XAnnotatedReference merge) {
        this.merge = merge;
    }

    /**
     * @since TODO
     */
    public void setRemove(XAnnotatedReference remove) {
        this.remove = remove;
    }

    @Override
    public void process(Context ctx, Element element, Object existing) {
        if (remove != null && Boolean.TRUE.equals(remove.getValue(ctx, element))) {
            setValue(ctx.getObject(), convertList(Collections.emptyList()));
            return;
        }
        List<Object> initList = new ArrayList<>();
        if (existing != null
                && (!hasValue(ctx, element) || merge == null || Boolean.TRUE.equals(merge.getValue(ctx, element)))) {
            Object[] currentListValue = getCurrentListValue(existing);
            if (currentListValue != null) {
                initList.addAll(Arrays.asList(currentListValue));
            }
        }
        Object value = getValue(ctx, element, initList);
        if (value != null) {
            setValue(ctx.getObject(), value);
        }
    }

    @Override
    public Object getValue(Context ctx, Element base) {
        return getValue(ctx, base, new ArrayList<>());
    }

    protected Object getValue(Context ctx, Element base, List<Object> values) {
        if (xao != null) {
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

    @SuppressWarnings("unchecked")
    protected Object convertList(List<Object> values) {
        if (isNullByDefault && values.isEmpty()) {
            return null;
        }

        if (type != ArrayList.class) {
            if (type.isArray()) {
                if (componentType.isPrimitive()) {
                    // primitive arrays cannot be casted to Object[]
                    return PrimitiveArrays.toPrimitiveArray(values, componentType);
                } else {
                    return values.toArray((Object[]) Array.newInstance(componentType, values.size()));
                }
            } else {
                try {
                    Collection<Object> col = (Collection<Object>) type.getDeclaredConstructor().newInstance();
                    col.addAll(values);
                    return col;
                } catch (ReflectiveOperationException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }

        return values;
    }

    protected Object[] getCurrentListValue(Object instance) {
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
            return objects;
        }
        return null;
    }

    @Override
    public void toXML(Object instance, Element parent) {
        Object[] objects = getCurrentListValue(instance);
        if (objects != null) {
            if (xao == null) {
                for (Object o : objects) {
                    String value = valueFactory.serialize(null, o);
                    if (value != null) {
                        Element e = XMLBuilder.addElement(parent, path);
                        XMLBuilder.fillField(e, value, path.attribute);
                    }
                }
            } else {
                for (Object o : objects) {
                    Element e = XMLBuilder.addElement(parent, path);
                    XMLBuilder.toXML(o, e, xao);
                }
            }
        }
    }
}

class ElementVisitor implements DOMHelper.NodeVisitor {

    @Override
    public void visitNode(Context ctx, XAnnotatedMember xam, Node node, Collection<Object> result) {
        result.add(xam.xao.newInstance(ctx, (Element) node));
    }

}

class ElementValueVisitor implements DOMHelper.NodeVisitor {
    @Override
    public void visitNode(Context ctx, XAnnotatedMember xam, Node node, Collection<Object> result) {
        String val = node.getTextContent();
        if (xam.trim) {
            val = val.trim();
        }
        if (xam.valueFactory != null) {
            result.add(xam.valueFactory.deserialize(ctx, val));
        } else {
            // TODO: log warning?
            result.add(val);
        }
    }
}

class AttributeValueVisitor implements DOMHelper.NodeVisitor {
    @Override
    public void visitNode(Context ctx, XAnnotatedMember xam, Node node, Collection<Object> result) {
        String val = node.getNodeValue();
        if (xam.valueFactory != null) {
            result.add(xam.valueFactory.deserialize(ctx, val));
        } else {
            // TODO: log warning?
            result.add(val);
        }
    }
}
