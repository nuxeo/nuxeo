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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.querydata;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Deprecated
public class DisplayExtensionConf implements Serializable {

    private static final long serialVersionUID = -8914165256577788155L;

    private static final String TAG_COLUMN = "column";

    private static final String ATTR_ID = "id";
    private static final String ATTR_LABEL = "label";
    private static final String ATTR_PROPERTY = "property";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_FORMAT = "format";

    private Element element;

    public Element getElement() {
        return element;
    }

    public void setElement(Element element) {
        this.element = element;
    }

    public Map<String, Map<String, String>> getColumns() {
        Map<String,  Map<String, String>> columns = new HashMap<String, Map<String, String>>();
        NodeList elements = element.getElementsByTagName(TAG_COLUMN);
        int len = elements.getLength();
        for (int i = 0; i < len; i++) {
            Element element = (Element) elements.item(i);
            String id = element.getAttribute(ATTR_ID);
            if (id != null) {
                Map<String , String> columninfo = new HashMap<String, String>();
                columninfo.put(ATTR_LABEL, element.getAttribute(ATTR_LABEL));
                columninfo.put(ATTR_PROPERTY, element.getAttribute(ATTR_PROPERTY));
                columninfo.put(ATTR_TYPE, element.getAttribute(ATTR_TYPE));
                columninfo.put(ATTR_FORMAT, element.getAttribute(ATTR_FORMAT));
                columns.put(id, columninfo);
            }
        }
        return columns;
    }

}
