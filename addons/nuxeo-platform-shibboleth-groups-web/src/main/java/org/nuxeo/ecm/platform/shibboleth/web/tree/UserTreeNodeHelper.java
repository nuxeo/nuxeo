/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.shibboleth.web.tree;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.shibboleth.web.service.ShibbolethGroupsService;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to provide some method with UserTreeNode
 *
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 * @see org.nuxeo.ecm.platform.shibboleth.web.tree.UserTreeNode
 */
public class UserTreeNodeHelper {

    /**
     * Build UserTreeNode Tree by parsing the name of all docs passed by argument. It uses Shibboleth Service parse char
     * definition to split doc name.
     *
     * @param docs list groups that want to be parsed
     * @return tree built, and an empty list if docs is null or empty.
     */
    public static List<UserTreeNode> getHierarcicalNodes(List<DocumentModel> docs) {
        List<UserTreeNode> root = new ArrayList<>();

        if (docs == null || docs.size() == 0) {
            return root;
        }

        String previousBasePath = null;
        List<DocumentModel> nodesToCreate = new ArrayList<>();
        for (DocumentModel doc : docs) {
            String id = doc.getId();
            int pos = id.lastIndexOf(getParseString());

            String currentBasePath;
            if (pos < 0) {
                currentBasePath = "";
            } else {
                currentBasePath = id.substring(0, pos + getParseStringLength());
            }

            if (previousBasePath != null && !currentBasePath.equals(previousBasePath)) {
                appendNodes(root, previousBasePath, nodesToCreate);
                nodesToCreate = new ArrayList<>();
            }
            nodesToCreate.add(doc);
            previousBasePath = currentBasePath;
        }
        appendNodes(root, previousBasePath, nodesToCreate);

        return root;
    }

    /**
     * Append and create UserTreeNode from nodesToCreate list to the rootList. It create or use defined node in the root
     * to create wanted nodes.
     *
     * @param root the root UserNodeTree List, may contains some part of the destination path
     * @param destinationPath destination path, not splitted.
     * @param nodesToCreate list of documentModel to be created at the destination path
     */
    protected static void appendNodes(List<UserTreeNode> root, String destinationPath, List<DocumentModel> nodesToCreate) {
        UserTreeNode currentNode = null;

        if (nodesToCreate == null || nodesToCreate.size() < 1) {
            return;
        }

        if (destinationPath == null || destinationPath.equals("")) {
            root.addAll(UserTreeNode.constructNodes(nodesToCreate));
            return;
        }

        for (String path : destinationPath.split(getParseString())) {
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
     *
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

    public static List<UserTreeNode> buildBranch(String branch, List<DocumentModel> docs) {
        List<UserTreeNode> nodeBranch = new ArrayList<>();
        appendNodes(nodeBranch, branch, docs);
        return nodeBranch;
    }

    public static String getParseString() {
        return getService().getParseString();
    }

    public static int getParseStringLength() {
        String str = getService().getParseString();
        return str == null ? -1 : str.length();
    }

    public static String getShibbGroupBasePath() {
        return getService().getShibbGroupBasePath();
    }

    protected static ShibbolethGroupsService getService() {
        return Framework.getService(ShibbolethGroupsService.class);
    }

}
