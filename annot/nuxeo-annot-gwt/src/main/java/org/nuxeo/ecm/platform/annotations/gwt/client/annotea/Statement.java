/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.annotea;

import com.google.gwt.xml.client.NamedNodeMap;
import com.google.gwt.xml.client.Node;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class Statement {

    private String subject;

    private String predicate;

    private String object;

    private boolean isResource;

    public Statement(Node node) {
        predicate = getFQName(node);
        if (node.getAttributes() != null
                && node.getAttributes().getLength() != 0) {
            NamedNodeMap map = node.getAttributes();
            for (int x = 0; x < map.getLength(); x++) {
                String attr = getFQName(map.item(x));
                if (attr.equals(RDFConstant.R_RESOURCE)) {
                    object = map.item(x).getNodeValue();
                    isResource = true;
                }
            }
        }
        if (!isResource) {
            if (node.getChildNodes() != null
                    && node.getChildNodes().getLength() != 0) {
                object = node.getChildNodes().item(0).getNodeValue().trim();
            } else {
                object = "";
            }
        }

    }

    public boolean isResource() {
        return isResource;
    }

    public void setResource(boolean isResource) {
        this.isResource = isResource;
    }

    private String getFQName(Node node) {
        String ns = node.getNamespaceURI();
        String name = node.getNodeName();
        name = name.replaceFirst(".*:", "");
        return "{" + ns + "}" + name;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPredicate() {
        return predicate;
    }

    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }
}
