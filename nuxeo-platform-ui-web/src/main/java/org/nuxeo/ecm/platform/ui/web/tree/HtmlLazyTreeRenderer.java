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

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.custom.tree2.HtmlTree;
import org.apache.myfaces.custom.tree2.HtmlTreeRenderer;
import org.apache.myfaces.custom.tree2.TreeState;
import org.apache.myfaces.custom.tree2.TreeWalker;
import org.apache.myfaces.shared_tomahawk.renderkit.html.HTML;
import org.apache.myfaces.shared_tomahawk.renderkit.html.HtmlRendererUtils;

public class HtmlLazyTreeRenderer extends HtmlTreeRenderer {

    private static final Log log = LogFactory.getLog(HtmlLazyTreeRenderer.class);

    public void encodeChildren(FacesContext context, UIComponent component)
            throws IOException {
        HtmlTree tree = (HtmlTree) component;

        if (!component.isRendered()) {
            return;
        }
        if (tree.getValue() == null) {
            return;
        }

        ResponseWriter out = context.getResponseWriter();
        String clientId = null;

        if (component.getId() != null
                && !component.getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX)) {
            clientId = component.getClientId(context);
        }

        boolean isOuterSpanUsed = false;

        if (clientId != null) {
            isOuterSpanUsed = true;
            out.startElement("span", component);
            out.writeAttribute("id", clientId, "id");
        }

        boolean clientSideToggle = tree.isClientSideToggle();
        boolean showRootNode = tree.isShowRootNode();

        TreeState state = tree.getDataModel().getTreeState();
        TreeWalker walker = tree.getDataModel().getTreeWalker();
        walker.reset();
        walker.setTree(tree);

        walker.setCheckState(!clientSideToggle); // walk all nodes in client mode

        if (showRootNode) {
            // encode the tree (starting with the root node)
            encodeTree(context, out, tree, walker);
        } else {
            // mark the root as expanded (so we don't stop there)
            String rootNodeId = walker.getRootNodeId();
            if (!state.isNodeExpanded(rootNodeId)) {
                state.toggleExpanded(rootNodeId);
            }

            // skip the root node
            int childCount = ((LazyTreeWalker) walker).nextCount();

            // now encode each of the nodes in the level immediately below the root
            for (int i = 0; i < childCount; i++) {
                encodeTree(context, out, tree, walker);
            }
        }

        // reset the current node id once we're done encoding everything
        tree.setNodeId(null);

        if (isOuterSpanUsed) {
            out.endElement("span");
        }
    }

    protected void encodeTree(FacesContext context, ResponseWriter out,
            HtmlTree tree, TreeWalker walker) throws IOException {
        int childCount = ((LazyTreeWalker) walker).nextCount();

        boolean clientSideToggle = tree.isClientSideToggle();

        // encode the current node
        HtmlRendererUtils.writePrettyLineSeparator(context);
        beforeNodeEncode(context, out, tree);
        encodeCurrentNode(context, out, tree);
        afterNodeEncode(context, out);

        // if client side toggling is on, add a span to be used for displaying/hiding children
        if (clientSideToggle) {
            String spanId = TOGGLE_SPAN + ":" + tree.getClientId(context) + ":"
                    + tree.getNodeId();

            out.startElement(HTML.SPAN_ELEM, tree);
            out.writeAttribute(HTML.ID_ATTR, spanId, null);

            if (tree.isNodeExpanded()) {
                out.writeAttribute(HTML.STYLE_ATTR, "display:block", null);
            } else {
                out.writeAttribute(HTML.STYLE_ATTR, "display:none", null);
            }
        }

        for (int i = 0; i < childCount; i++) {
            encodeTree(context, out, tree, walker);
        }

        if (clientSideToggle) {
            out.endElement(HTML.SPAN_ELEM);
        }
    }

}
