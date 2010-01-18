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
 * $Id: LifeCycleTypesConfiguration.java 16207 2007-04-15 11:56:45Z sfermigier $
 */

package org.nuxeo.ecm.core.lifecycle.extensions;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Configuration helper class for types.
 *
 * @see org.nuxeo.ecm.core.lifecycle.impl.LifeCycleServiceImpl
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class LifeCycleTypesConfiguration {

    private static final String TAG_TYPE = "type";
    private static final String ATTR_TYPE_NAME = "name";

    /** The DOM element holding the states. */
    private Element element;

    public LifeCycleTypesConfiguration(Element element) {
        this.element = element;
    }

    public Element getElement() {
        return element;
    }

    public void setElement(Element element) {
        this.element = element;
    }

    public Map<String, String> getTypesMapping() {
        Map<String, String> typesMapping = new HashMap<String, String>();
        NodeList elements = element.getElementsByTagName(TAG_TYPE);
        int len = elements.getLength();
        for (int i = 0; i < len; i++) {
            Element element = (Element) elements.item(i);
            String typeName = element.getAttribute(ATTR_TYPE_NAME);
            String lifeCycleName = element.getTextContent();
            typesMapping.put(typeName, lifeCycleName);
        }
        return typesMapping;
    }

}
