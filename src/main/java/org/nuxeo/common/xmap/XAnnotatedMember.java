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

import org.nuxeo.common.xmap.annotation.XNode;
import org.w3c.dom.Element;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class XAnnotatedMember {

    protected final XAccessor accessor;

    protected Path path;

    protected boolean trim;

    /** The Java type of the described element. */
    protected Class type;

    /** Not null if the described object is an xannotated object. */
    protected XAnnotatedObject xao;

    /**
     * The value factory used to transform strings in objects compatible with
     * this member type. In the case of collection types this factory is used
     * for collection components.
     */
    protected XValueFactory valueFactory;

    private final XMap xmap;

    protected XAnnotatedMember(XMap xmap, XAccessor accessor) {
        this.xmap = xmap;
        this.accessor = accessor;
    }

    public XAnnotatedMember(XMap xmap, XAccessor setter, XNode anno) {
        this.xmap = xmap;
        accessor = setter;
        path = new Path(anno.value());
        trim = anno.trim();
        type = setter.getType();
        valueFactory = xmap.getValueFactory(type);
        xao = xmap.register(type);
    }

    protected void setValue(Object instance, Object value) throws Exception {
        try {
            accessor.setValue(instance, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("%s, setter=%s, value=%s", e.getMessage(),
                            accessor, value), e);
        }
    }

    public void toXML(Object instance, Element parent) throws Exception{
        Element e = XMLBuilder.getOrCreateElement(parent, path);
        Object v = accessor.getValue(instance);
        if (xao == null ) {
            if ( v != null && valueFactory != null){
                String value = valueFactory.serialize(null,v);
                if ( value != null) {

                    XMLBuilder.fillField(e, value, path.attribute);
                }
            }
        } else {
            XMLBuilder.toXML(v, e, xao);
        }
    }

    public void process(Context ctx, Element element) throws Exception {
        Object value = getValue(ctx, element);
        if (value != null) {
            setValue(ctx.getObject(), value);
        }
    }

    protected Object getValue(Context ctx, Element base) throws Exception {
        if (xao != null) {
            Element el = (Element) DOMHelper.getElementNode(base, path);
            return el == null ? null : xao.newInstance(ctx, el);
        }
        // scalar field
        if (type == Element.class) {
            // allow DOM elements as values
            return base;
        }
        String val = DOMHelper.getNodeValue(base, path);
        if (val != null) {
            if (trim) {
                val = val.trim();
            }
            return valueFactory.deserialize(ctx, val);
        }
        return null;
    }

}
