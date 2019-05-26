/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.annotea;

import com.google.gwt.xml.client.NamedNodeMap;
import com.google.gwt.xml.client.Node;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class Statement {

    private String subject;

    private String predicate;

    private String object;

    private boolean isResource;

    public Statement(Node node) {
        predicate = getFQName(node);
        if (node.getAttributes() != null && node.getAttributes().getLength() != 0) {
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
            if (node.getChildNodes() != null && node.getChildNodes().getLength() != 0) {
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
