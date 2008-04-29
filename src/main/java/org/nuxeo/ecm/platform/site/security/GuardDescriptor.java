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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.site.security;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.site.security.guards.And;
import org.nuxeo.ecm.platform.site.security.guards.FacetGuard;
import org.nuxeo.ecm.platform.site.security.guards.GroupGuard;
import org.nuxeo.ecm.platform.site.security.guards.PermissionGuard;
import org.nuxeo.ecm.platform.site.security.guards.SchemaGuard;
import org.nuxeo.ecm.platform.site.security.guards.ScriptGuard;
import org.nuxeo.ecm.platform.site.security.guards.TypeGuard;
import org.nuxeo.ecm.platform.site.security.guards.UserGuard;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("permission")
public class GuardDescriptor {

    @XNode("@id")
    protected String id;

    @XNode("@expression")
    protected String expression;

    protected Map<String, Guard> guards;


    public GuardDescriptor() {
        this (null);
    }

    public GuardDescriptor(String name) {
        this.id = name;
        this.guards = new HashMap<String, Guard>();
    }

    /**
     * @return the guards.
     */
    public Map<String, Guard> getGuards() {
        return guards;
    }

    @XContent
    protected void setGuards(DocumentFragment content) {
        Node node = content.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String name = node.getNodeName();
                if ("guard".equals(name)) {
                    NamedNodeMap map = node.getAttributes();
                    Node aId = map.getNamedItem("id");
                    Node aType = map.getNamedItem("type");
                    if (aId == null) {
                        throw new IllegalArgumentException("id is required");
                    }
                    String id = aId.getNodeValue();
                    if (aType == null) {
                        throw new IllegalArgumentException("type is required");
                    } else {
                        //String value = node.getTextContent().trim();
                        //guards.put(id, new ScriptGuard(value));
                        //TODO: compound guard
                    }
                    String type = aType.getNodeValue();
                    if ("permission".equals(type)) {
                        String value = node.getTextContent().trim();
                        guards.put(id, new PermissionGuard(value));
                    } else if ("facet".equals(type)) {
                        String value = node.getTextContent().trim();
                        guards.put(id, new FacetGuard(value));
                    } else if ("type".equals(type)) {
                        String value = node.getTextContent().trim();
                        guards.put(id, new TypeGuard(value));
                    } else if ("schema".equals(type)) {
                        String value = node.getTextContent().trim();
                        guards.put(id, new SchemaGuard(value));
                    } else if ("user".equals(type)) {
                        String value = node.getTextContent().trim();
                        guards.put(id, new UserGuard(value));
                    } else if ("group".equals(type)) {
                        String value = node.getTextContent().trim();
                        guards.put(id, new GroupGuard(value));
                    } else if ("script".equals(type)) {
                        String value = node.getTextContent().trim();
                        guards.put(id, new ScriptGuard(value));
                    }
                }
            }
            node = node.getNextSibling();
        }
    }

    public Guard getGuard() throws ParseException {
        if (expression == null || expression.length() == 0) {
            return new And(guards.values());
        }
        return PermissionService.getInstance().parse(expression, guards);
    }

    /**
     * @return the name.
     */
    public String getId() {
        return id;
    }

    public Permission getPermission() throws ParseException {
        return new DefaultPermission(id, getGuard());
    }

    public static GuardDescriptor build(Node element) {
        NamedNodeMap attrs = element.getAttributes();
        Node idNode = attrs.getNamedItem("id");
        String id = null;
        if (idNode != null) {
            id = idNode.getNodeValue();
        }
        GuardDescriptor gd = new GuardDescriptor(id);

        Node node = element.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String name = node.getNodeName();
                if ("permission".equals(name)) {
                    Node attr = attrs.getNamedItem("id");
                    if (attr != null) {
                        String value = node.getTextContent().trim();
                        gd.guards.put(attr.getNodeValue(), new PermissionGuard(value));
                    }
                } else if ("facet".equals(name)) {
                    NamedNodeMap map = node.getAttributes();
                    Node attr = map.getNamedItem("id");
                    if (attr != null) {
                        String value = node.getTextContent().trim();
                        gd.guards.put(attr.getNodeValue(), new FacetGuard(value));
                    }
                } else if ("type".equals(name)) {
                    NamedNodeMap map = node.getAttributes();
                    Node attr = map.getNamedItem("id");
                    if (attr != null) {
                        String value = node.getTextContent().trim();
                        gd.guards.put(attr.getNodeValue(), new TypeGuard(value));
                    }
                } else if ("schema".equals(name)) {
                    NamedNodeMap map = node.getAttributes();
                    Node attr = map.getNamedItem("id");
                    if (attr != null) {
                        String value = node.getTextContent().trim();
                        gd.guards.put(attr.getNodeValue(), new SchemaGuard(value));
                    }
                } else if ("user".equals(name)) {
                    NamedNodeMap map = node.getAttributes();
                    Node attr = map.getNamedItem("id");
                    if (attr != null) {
                        String value = node.getTextContent().trim();
                        gd.guards.put(attr.getNodeValue(), new UserGuard(value));
                    }
                } else if ("group".equals(name)) {
                    NamedNodeMap map = node.getAttributes();
                    Node attr = map.getNamedItem("id");
                    if (attr != null) {
                        String value = node.getTextContent().trim();
                        gd.guards.put(attr.getNodeValue(), new GroupGuard(value));
                    }
                } else if ("script".equals(name)) {
                    NamedNodeMap map = node.getAttributes();
                    Node attr = map.getNamedItem("id");
                    if (attr != null) {
                        String value = node.getTextContent().trim();
                        gd.guards.put(attr.getNodeValue(), new ScriptGuard(value));
                    }
//                } else if ("guard".equals(name)) {
//                    GuardDescriptor subGd = build(node);
//                    gd.guards.put(subGd.getId(), subGd);
                }

            }
        }

        return gd;
    }

}
