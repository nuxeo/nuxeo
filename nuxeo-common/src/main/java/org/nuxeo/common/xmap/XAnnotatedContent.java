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

import java.io.IOException;

import org.nuxeo.common.xmap.annotation.XContent;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ranges.DocumentRange;
import org.w3c.dom.ranges.Range;

import org.apache.xml.serialize.OutputFormat;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class XAnnotatedContent extends XAnnotatedMember {

    private static final OutputFormat DEFAULT_FORMAT = new OutputFormat();

    static {
        DEFAULT_FORMAT.setOmitXMLDeclaration(true);
        DEFAULT_FORMAT.setIndenting(true);
        DEFAULT_FORMAT.setMethod("xml");
        DEFAULT_FORMAT.setEncoding("UTF-8");
    }


    public XAnnotatedContent(XMap xmap, XAccessor setter, XContent anno) {
        super(xmap, setter);
        path = new Path(anno.value());
        type = setter.getType();
        valueFactory = xmap.getValueFactory(type);
        xao = xmap.register(type);
    }

    @Override
    protected Object getValue(Context ctx, Element base) {
        Element el = (Element) DOMHelper.getElementNode(base, path);
        if (el == null) {
            return null;
        }
        el.normalize();
        Node node = el.getFirstChild();
        if (node == null) {
            boolean asDOM = accessor.getType() == DocumentFragment.class;
            return asDOM ? null : "";
        }
        Range range = ((DocumentRange) el.getOwnerDocument()).createRange();
        range.setStartBefore(node);
        range.setEndAfter(el.getLastChild());
        DocumentFragment fragment = range.cloneContents();
        boolean asDOM = accessor.getType() == DocumentFragment.class;
        if (asDOM) {
            return fragment;
        } else {
            try {
                return DOMSerializer.toString(fragment, DEFAULT_FORMAT);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    @Override
    public void toXML(Object instance, Element parent) {
        Object v = accessor.getValue(instance);
        if (v instanceof DocumentFragment) {
            Element e = XMLBuilder.getOrCreateElement(parent, path);
            DocumentFragment df = (DocumentFragment) v;
            Node node = e.getOwnerDocument().importNode(df, true);
            e.appendChild(node);
        } else if (valueFactory != null && v != null) {
            String value = valueFactory.serialize(null, v);
            if (value != null) {
                Element e = XMLBuilder.getOrCreateElement(parent, path);
                DOMHelper.loadFragment(e, value);
            }
        }
    }

}
