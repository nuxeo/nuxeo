/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.common.xmap;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.common.collections.PrimitiveArrays;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@SuppressWarnings({"SuppressionAnnotation"})
public class XAnnotatedList extends XAnnotatedMember {

    protected static final ElementVisitor elementListVisitor = new ElementVisitor();
    protected static final ElementValueVisitor elementVisitor = new ElementValueVisitor();
    protected static final AttributeValueVisitor attributeVisitor = new AttributeValueVisitor();

    // indicates the type of the collection components
    protected Class componentType;

    protected boolean isNullByDefault;


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

    @SuppressWarnings("unchecked")
    @Override
    protected Object getValue(Context ctx, Element base) throws Exception {
        List<Object> values = new ArrayList<Object>();
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

        if (type != ArrayList.class) {
            if (type.isArray()) {
                if (componentType.isPrimitive()) {
                    // primitive arrays cannot be casted to Object[]
                    return PrimitiveArrays.toPrimitiveArray(values, componentType);
                } else {
                    return values.toArray((Object[]) Array.newInstance(componentType, values.size()));
                }
            } else {
                Collection col = (Collection) type.newInstance();
                col.addAll(values);
                return col;
            }
        }

        if (isNullByDefault && values.isEmpty()) {
            values = null;
        }
        return values;
    }

    @Override
    public void toXML(Object instance, Element parent) throws Exception {
        Object v = accessor.getValue(instance);
        if ( v != null ){
            Object[] objects = null;
            if ( v instanceof Object[]){
                objects = (Object[]) v;
            } else if (v instanceof List){
                objects = ((List)v).toArray();
            } else {
                objects = PrimitiveArrays.toObjectArray(v);
            }
            if ( objects != null) {
                if ( xao == null){
                    for ( Object o : objects){
                        String value = valueFactory.serialize(null,o);
                        if ( value != null ) {
                            Element e = XMLBuilder.addElement(parent, path);
                            XMLBuilder.fillField(e, value, path.attribute);
                        }
                    }
                } else {
                    for ( Object o : objects){
                        Element e = XMLBuilder.addElement(parent, path);
                        XMLBuilder.toXML(o, e, xao);
                    }
                }
            }
        }
    }
}
    class ElementVisitor implements DOMHelper.NodeVisitor {
        public void visitNode(Context ctx, XAnnotatedMember xam, Node node, Collection<Object> result) {
            try {
                result.add(xam.xao.newInstance(ctx, (Element) node));
            } catch (Exception e) {
                // TODO
                e.printStackTrace();
            }
        }
    }

    class ElementValueVisitor implements DOMHelper.NodeVisitor {
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
