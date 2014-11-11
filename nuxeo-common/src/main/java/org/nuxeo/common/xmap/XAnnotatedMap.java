/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@SuppressWarnings( { "SuppressionAnnotation" })
public class XAnnotatedMap extends XAnnotatedList {

    protected static final ElementMapVisitor elementMapVisitor = new ElementMapVisitor();

    protected static final ElementValueMapVisitor elementVisitor = new ElementValueMapVisitor();

    protected static final AttributeValueMapVisitor attributeVisitor = new AttributeValueMapVisitor();

    protected final Path key;

    protected final boolean isNullByDefault;

    public XAnnotatedMap(XMap xmap, XAccessor setter, XNodeMap anno) {
        super(xmap, setter);
        path = new Path(anno.value());
        trim = anno.trim();
        key = new Path(anno.key());
        type = anno.type();
        componentType = anno.componentType();
        valueFactory = xmap.getValueFactory(componentType);
        xao = xmap.register(componentType);
        isNullByDefault = anno.nullByDefault();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object getValue(Context ctx, Element base)
            throws IllegalAccessException, InstantiationException {
        Map<String, Object> values = (Map) type.newInstance();
        if (xao != null) {
            DOMHelper.visitMapNodes(ctx, this, base, path, elementMapVisitor,
                    values);
        } else {
            if (path.attribute != null) {
                // attribute list
                DOMHelper.visitMapNodes(ctx, this, base, path,
                        attributeVisitor, values);
            } else {
                // element list
                DOMHelper.visitMapNodes(ctx, this, base, path, elementVisitor,
                        values);
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
        if (v != null && v instanceof Map<?, ?>) {
            Map<String, ?> map = (Map<String, ?>) v;
            if (xao == null) {
                for (Map.Entry<String, ?> entry : map.entrySet()) {
                    String entryKey = entry.getKey();
                    String value = valueFactory.serialize(null,
                            entry.getValue());
                    Element e = XMLBuilder.addElement(parent, path);
                    Element keyElement = XMLBuilder.getOrCreateElement(e, key);
                    XMLBuilder.fillField(keyElement, entryKey, key.attribute);
                    XMLBuilder.fillField(e, value, null);
                }
            } else {
                for (Map.Entry<String, ?> entry : map.entrySet()) {
                    String entryKey = entry.getKey();
                    Element e = XMLBuilder.addElement(parent, path);
                    Element keyElement = XMLBuilder.getOrCreateElement(e, key);
                    XMLBuilder.fillField(keyElement, entryKey, key.attribute);
                    XMLBuilder.toXML(entry.getValue(), e, xao);
                }
            }
        }
    }
}

class ElementMapVisitor implements DOMHelper.NodeMapVisitor {

    private static final Log log = LogFactory.getLog(ElementMapVisitor.class);

    public void visitNode(Context ctx, XAnnotatedMember xam, Node node,
            String key, Map<String, Object> result) {
        try {
            result.put(key, xam.xao.newInstance(ctx, (Element) node));
        } catch (Exception e) {
            log.error(e, e);
        }
    }
}

class ElementValueMapVisitor implements DOMHelper.NodeMapVisitor {
    public void visitNode(Context ctx, XAnnotatedMember xam, Node node,
            String key, Map<String, Object> result) {
        String val = node.getTextContent();
        if (xam.trim) {
            val = val.trim();
        }
        if (xam.valueFactory != null) {
            result.put(key, xam.valueFactory.deserialize(ctx, val));
        } else {
            // TODO: log warning?
            result.put(key, val);
        }
    }
}

class AttributeValueMapVisitor implements DOMHelper.NodeMapVisitor {
    public void visitNode(Context ctx, XAnnotatedMember xam, Node node,
            String key, Map<String, Object> result) {
        String val = node.getNodeValue();
        if (xam.valueFactory != null) {
            result.put(key, xam.valueFactory.deserialize(ctx, val));
        } else {
            // TODO: log warning?
            result.put(key, val);
        }
    }
}
