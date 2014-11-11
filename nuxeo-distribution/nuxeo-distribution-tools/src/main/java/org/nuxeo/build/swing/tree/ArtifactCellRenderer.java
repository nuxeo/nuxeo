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
 */
package org.nuxeo.build.swing.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.nuxeo.build.maven.graph.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ArtifactCellRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = 1L;

    protected ArtifactTree tree;

    public ArtifactCellRenderer(ArtifactTree tree) {
        this.tree = tree;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean sel, boolean expanded, boolean leaf, int row,
            boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
                row, hasFocus);
        DefaultMutableTreeNode tn = (DefaultMutableTreeNode)value;
        Object o = tn.getUserObject();
        if (o instanceof Node) {
            Node node = (Node)o;
            processNode(this.tree.getProvider(), node, sel, expanded);
        }
        return this;
    }

    protected void processNode(ItemProvider provider, Node node, boolean isSelected, boolean isExpanded) {
        Icon icon = provider.getIcon(node, isExpanded);
        if (icon != null) {
            setIcon(icon);
        }
        setText(provider.getName(node));
        setToolTipText(provider.getTooltip(node));
        Color color = provider.getForegroundColor(node, isSelected);
        if (color != null) {
            setForeground(color);
        }
        color = provider.getBackgroundColor(node, isSelected);
        if (color != null) {
            setBackground(color);
        }
        Font font = provider.getFont(node);
        if (font != null) {
            setFont(font);
        }
    }
}
