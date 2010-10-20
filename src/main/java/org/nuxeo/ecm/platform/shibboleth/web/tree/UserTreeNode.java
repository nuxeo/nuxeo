/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.shibboleth.web.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Tree node class handling node information and Nodes operation
 *
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
public class UserTreeNode {

    public static final String USER = "user";

    public static final String GROUP = "group";

    public static final String SHIBB_GROUP = "shibbGroup";

    protected DocumentModel document;

    protected String type;

    protected List<UserTreeNode> childrens;

    protected String name = "";

    public UserTreeNode(String name) {
        this.name = name;
    }

    public UserTreeNode(DocumentModel document) {
        this.document = document;
    }

    protected UserTreeNode() {
    }

    public List<UserTreeNode> getChildrens() {
        if (childrens == null) {
            childrens = new ArrayList<UserTreeNode>();
        }
        return childrens;
    }

    public void addChildren(UserTreeNode child) {
        getChildrens().add(child);
    }

    public String getId() {
        return document == null ? name : document.getId();
    }

    /**
     * Get the displayed name, if instantiate with a documentModel it
     * will be the document Id
     *
     * @return name defined with the constructor, or Document Id
     */
    public String getDisplayedName() {
        if (name.equals("") && document != null) {
            String id = document.getId();
            int pos = id.lastIndexOf(":");

            if (pos > 0) {
                name = id.substring(pos + 1);
            } else{
                name = id;
            }
        }
        return name;
    }

    /**
     * Factory method to build a collection of UserTReeNode.
     *
     * @return empty list if no docs passed
     */
    public static List<UserTreeNode> constructNodes(Collection<DocumentModel> docs) {
        List<UserTreeNode> ret = new ArrayList<UserTreeNode>();

        if (docs != null) {
            for(DocumentModel doc : docs) {
                ret.add(new UserTreeNode(doc));
            }
        }

        return ret;
    }

    /**
     * Build UserTreeNode Tree by parsing the name of all docs passed by argument.
     * It uses Shibboleth Service parse char definition to split doc name.
     *
     * @param docs list groups that want to be parsed
     * @return tree built, and an empty list if docs is null or empty.
     */
    public static List<UserTreeNode> getHierarcicalNodes(List<DocumentModel> docs) {
        List<UserTreeNode> root = new ArrayList<UserTreeNode>();

        if (docs == null || docs.size() == 0) {
            return root;
        }

        String previousBasePath = null;
        List<DocumentModel> nodesToCreate = new ArrayList<DocumentModel>();
        for(DocumentModel doc : docs) {
            String id = doc.getId();
            int pos = id.lastIndexOf(":");

            String currentBasePath;
            if (pos < 0) {
                currentBasePath = "";
            } else {
                currentBasePath = id.substring(0, pos);
            }

            if (previousBasePath != null && !currentBasePath.equals(
                    previousBasePath)) {
                appendNodes(root, previousBasePath, nodesToCreate);
                nodesToCreate = new ArrayList<DocumentModel>();
            }
            nodesToCreate.add(doc);
            previousBasePath = currentBasePath;
        }
        appendNodes(root, previousBasePath, nodesToCreate);

        return root;
    }

    public static List<UserTreeNode> buildBranch(String branch, List<DocumentModel> docs) {
        List<UserTreeNode> nodeBranch = new ArrayList<UserTreeNode>();
        UserTreeNode.appendNodes(nodeBranch, branch, docs);
        return nodeBranch;
    }

    /**
     * Append and create UserTreeNode from nodesToCreate list to the rootList.
     * It create or use defined node in the root to create wanted nodes.
     *
     * @param root the root UserNodeTree List, may contains some part of the destination path
     * @param destinationPath destination path, not splitted.
     * @param nodesToCreate list of documentModel to be created at the destination path
     */
    protected static void appendNodes(List<UserTreeNode> root, String destinationPath,
            List<DocumentModel> nodesToCreate) {
        UserTreeNode currentNode = null;

        if (nodesToCreate == null || nodesToCreate.size() < 1) {
            return;
        }

        if (destinationPath == null || destinationPath.equals("")) {
            root.addAll(UserTreeNode.constructNodes(nodesToCreate));
            return;
        }

        for(String path : destinationPath.split(":")) {
            UserTreeNode node = searchNode(currentNode == null ? root : currentNode.getChildrens(), path);
            if (node == null) {
                node = new UserTreeNode(path);
                if (currentNode == null) {
                    root.add(node);
                } else {
                    currentNode.addChildren(node);
                }
            }
            currentNode = node;
        }

        assert currentNode != null;
        currentNode.getChildrens().addAll(UserTreeNode.constructNodes(nodesToCreate));
    }

    /**
     * Search a Node where param name is equals to the nodeId
     * @return null if not found
     */
    protected static UserTreeNode searchNode(List<UserTreeNode> from, String name) {
        for (UserTreeNode node : from) {
            if (node.getId().equals(name)) {
                return node;
            }
        }
        return null;
    }
}
