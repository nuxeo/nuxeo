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

package org.nuxeo.ecm.platform.ui.web.tree;

import java.util.Stack;

import org.apache.myfaces.custom.tree2.Tree;
import org.apache.myfaces.custom.tree2.TreeNode;
import org.apache.myfaces.custom.tree2.TreeWalker;

/**
 *
 * Walks the tree in a more lazy way to avoid loading tree 1 level deeper than
 * the current navigation.
 *
 * @author tiry
 *
 */
public class LazyTreeWalker implements TreeWalker {

    private static final String ROOT_NODE_ID = "0";
    private static final String TREE_NODE_SEPARATOR = ":";

    private Tree tree;
    private final Stack nodeInfoStack = new Stack();
    private boolean checkState = true;
    private boolean startedWalking = false;

    // see interface
    public void setTree(Tree tree) {
        this.tree = tree;
    }

    // see interface
    public boolean isCheckState() {
        return checkState;
    }

    // see interface
    public void setCheckState(boolean checkState) {
        this.checkState = checkState;
    }

    private static class NodeInfo {

        final String nodeId;
        final int childCount;
        int nextChildPos;

        NodeInfo(String nodeId, int childCount) {
            this.nodeId = nodeId;
            this.childCount = childCount;
            nextChildPos = 0;
        }

    }

    private int getChildCount(TreeNode node, String nodeId) {
        // don't render any children if the node is not expanded
        if (checkState && !tree.getDataModel().getTreeState().isNodeExpanded(nodeId)) {
            return 0;
        } else {
            return node.getChildCount();
        }
    }

    // see interface
    public boolean next() {
        return nextCount() != -1;
    }

    // see interface
    public int nextCount() {
        String nodeId;

        if (!startedWalking) {
            startedWalking = true;
            nodeId = ROOT_NODE_ID;
        } else {
            NodeInfo nodeInfo;
            while (true) {
                nodeInfo = (NodeInfo) nodeInfoStack.peek();
                if (nodeInfo.nextChildPos < nodeInfo.childCount) {
                    break;
                }
                nodeInfoStack.pop();
                if (nodeInfoStack.isEmpty()) {
                    return -1;
                }
            }
            nodeId = nodeInfo.nodeId + TREE_NODE_SEPARATOR + nodeInfo.nextChildPos;
            nodeInfo.nextChildPos++;
        }
        tree.setNodeId(nodeId);
        TreeNode node = tree.getNode();
        int childCount = getChildCount(node, nodeId);
        NodeInfo nodeInfo = new NodeInfo(nodeId, childCount);
        nodeInfoStack.push(nodeInfo);
        return childCount;
    }

    // see interface
    public String getRootNodeId() {
        return ROOT_NODE_ID;
    }

    // see interface
    public void reset() {
        nodeInfoStack.empty();
        startedWalking = false;
    }

}
