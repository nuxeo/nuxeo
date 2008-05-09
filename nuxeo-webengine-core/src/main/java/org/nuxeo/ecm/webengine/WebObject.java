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

package org.nuxeo.ecm.webengine;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.actions.ActionDescriptor;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WebObject {

    protected WebObject next;
    protected WebObject prev;

    protected DocumentModel doc;
    protected final String name;
    protected final WebContext context;
    protected ObjectDescriptor desc;


    public WebObject(WebContext context, String name, DocumentModel doc) {
        this.context = context;
        this.name = name;
        this.doc = doc;
    }

    public final ActionDescriptor getAction(String action) {
        ObjectDescriptor desc = getDescriptor();
        if (desc != null) {
            return desc.getAction(action);
        }
        return null;
    }

    public final Collection<ActionDescriptor> getActions() throws WebException {
        ObjectDescriptor desc = getDescriptor();
        if (desc != null) {
            return desc.getEnabledActions(this);
        }
        return null;
    }

    public final Collection<ActionDescriptor> getActions(String category) throws WebException {
        ObjectDescriptor desc = getDescriptor();
        if (desc != null) {
            return desc.getEnabledActions(this, category);
        }
        return null;
    }

    public final Map<String, Collection<ActionDescriptor>> getActionsByCategory() throws WebException {
        ObjectDescriptor desc = getDescriptor();
        if (desc != null) {
            return desc.getEnabledActionsByCategory(this);
        }
        return null;
    }

    public String getActionScript(String action) {
        ActionDescriptor desc = getAction(action);
        String path;
        if (desc != null) {
            path = desc.getScript();
            if (path != null) {
                return path;
            }
        }
        WebRoot root = context.getRoot();
        if (doc != null) {
            String type = doc.getType();
            path = type + '/' + action + ".ftl";
            File file = root.getFile(path);
            if (file.isFile()) {
                return path;
            }
        }
        return "default/"+action+".ftl";
    }

    /**
     * @return the request.
     */
    public WebContext getWebContext() {
        return context;
    }

    public final String getName() {
        return name;
    }

    public final DocumentModel getDocument() {
        return doc;
    }

    public boolean isResolved() {
        return doc != null;
    }

    public final WebObject next() {
        return next;
    }

    public final WebObject prev() {
        return prev;
    }

    public boolean isRoot() {
        return context.getFirstObject() == this;
    }

    public final boolean isLast() {
        return context.getLastObject() == this;
    }

    public final boolean isLastResolved() {
        return context.getLastResolvedObject() == this;
    }

    public final ObjectDescriptor getDescriptor() {
        if (desc == null && doc != null) {
            desc = context.getWebEngine().getInstanceOf(doc.getDocumentType());
        }
        return desc;
    }

    public final RequestHandler getRequestHandler() throws WebException {
        ObjectDescriptor desc = getDescriptor();
        if (desc != null) {
            return desc.getRequestHandler();
        }
        return null;
    }

    public String getPath() {
        StringBuilder buf = new StringBuilder();
        collectPath(buf);
        return buf.toString();
    }

    public String getAbsolutePath() {
        StringBuilder buf = new StringBuilder(context.getSitePath());
        collectPath(buf);
        return buf.toString();
    }

    protected void collectPath(StringBuilder buf) {
        if (prev != null) {
            prev.collectPath(buf);
        }
        buf.append('/').append(name);
    }

    public boolean resolve(DocumentModel doc) {
        return context.resolveObject(this, doc);
    }

    public boolean traverse() throws WebException {
        ObjectDescriptor desc = getDescriptor();
        return desc != null ? desc.getRequestHandler().traverse(this) : false;
    }

    @Override
    public String toString() {
        return name + " [ " + (isResolved() ? doc.getPath() : "unresolved") + " ]";
    }

}
