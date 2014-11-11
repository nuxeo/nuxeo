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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.nuxeo.ecm.core.api.DocumentException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class JCRHelper {

    static int cnt = 0;

    public static void saveNode(Node node) throws DocumentException {
        try {
            if (node.isNew()) {
                do  {
                    node = node.getParent();
                } while (node.isNew());
            }
            if (node.isModified()) {
                node.save();
            } // else do nothing
        } catch (RepositoryException e) {
            throw new DocumentException("Saving node failed", e);
        }
    }

    public static void saveNodeIfNew(Node node) throws DocumentException {
        if (node.isNew()) {
            try {
                do  {
                    node = node.getParent();
                } while (node.isNew());
                if (node.isModified()) {
                    node.save();
                }
            } catch (RepositoryException e) {
                throw new DocumentException("Saving node failed", e);
            }
        }
    }

    public static Node copy(JCRDocument doc, JCRDocument target)
            throws DocumentException {
        try {
            return copy(doc.getNode(),
                    ModelAdapter.getContainerNode(target.getNode()),
                    doc.getName() + '_' + doc.getUUID() + '_' + (cnt++));
        } catch (RepositoryException e) {
            throw new DocumentException("failed to copy node", e);
        }
    }

    public static Node copy(Node node, Node target) throws RepositoryException {
        return copy(node, target, null);
    }

    public static Node copy(Node node, Node target, String name) throws RepositoryException {
        NodeType type = node.getPrimaryNodeType();
        NodeType[] mix = node.getMixinNodeTypes();
        if (name == null) {
            name = node.getName();
        }
        Node newNode = null;
        if (target.hasNode(name)) { // this may happen for autocreated nodes
            newNode = target.getNode(name);
        } else if (!node.getDefinition().isAutoCreated()) {
            newNode = target.addNode(name, type.getName());
        }
        for (NodeType nt : mix) {
            newNode.addMixin(nt.getName());
        }
        javax.jcr.PropertyIterator it = node.getProperties();
        while (it.hasNext()) {
            Property prop = it.nextProperty();
            if (prop.getDefinition().isProtected()) {
                //System.out.println(">>> ignore prop: " + prop.getName());
                continue; // ignore protected nodes
            }
            //System.out.println(">>> copy prop: " + prop.getName());
            if (prop.getDefinition().isMultiple()) {
                newNode.setProperty(prop.getName(), prop.getValues());
            } else {
                newNode.setProperty(prop.getName(), prop.getValue());
            }
        }
        NodeIterator subnodes = node.getNodes();
        while (subnodes.hasNext()) {
            Node subNode = subnodes.nextNode();
            //System.out.println(">>> copy node: " + subNode.getName());
            copy(subNode, newNode);
        }
        return newNode;
    }

}
