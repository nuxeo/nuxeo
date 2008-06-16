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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.actions.ActionDescriptor;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WebObject {

    protected final static Log log = LogFactory.getLog(WebObject.class);

    protected String urlPath;

    protected WebObject next;
    protected WebObject prev;

    protected DocumentModel doc;
    protected final String name;
    protected final WebContext context;
    protected WebObjectDescriptor desc;


    public WebObject(WebContext context, String name, DocumentModel doc) {
        if (context == null  || doc == null || name == null) {
            throw new IllegalArgumentException("Failed to create WebObject. " +
                    "All three constructor arguments must be non null: "+context +","+ name+","+doc);
        }
        this.context = context;
        this.name = name;
        this.doc = doc;
    }

    public final ActionDescriptor getAction(String action) {
        WebObjectDescriptor desc = getDescriptor();
        if (desc != null) {
            return desc.getAction(action);
        }
        return null;
    }

    public final Collection<ActionDescriptor> getActions() throws WebException {
        WebObjectDescriptor desc = getDescriptor();
        if (desc != null) {
            return desc.getEnabledActions(this);
        }
        return null;
    }

    public final Collection<ActionDescriptor> getActions(String category) throws WebException {
        WebObjectDescriptor desc = getDescriptor();
        if (desc != null) {
            return desc.getEnabledActions(this, category);
        }
        return null;
    }

    public final Map<String, Collection<ActionDescriptor>> getActionsByCategory() throws WebException {
        WebObjectDescriptor desc = getDescriptor();
        if (desc != null) {
            return desc.getEnabledActionsByCategory(this);
        }
        return null;
    }

    public ScriptFile getActionScript(String action) throws IOException {
        ActionDescriptor desc = getAction(action);
        String path;
        if (desc != null) {
            path = desc.getScript();
            if (path != null) {
                ScriptFile file = context.getFile(path);
                if (file != null) {
                    return file;
                } else {
                    log.warn("Action script not found: "+path);
                }
            }
        }
        if (doc != null) {
            ScriptFile file = context.getApplication().getActionScript(action, doc.getDocumentType());
            if (file != null) {
                return file;
            }
        }
        return null;
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

    public final String getTitle() {
        return doc == null ? name : doc.getTitle();
    }

    public final DocumentModel getDocument() {
        return doc;
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

    public final WebObjectDescriptor getDescriptor() {
        if (desc == null && doc != null) {
            desc = context.getApplication().getObjectDescriptor(doc.getDocumentType());
        }
        return desc;
    }

    public final RequestHandler getRequestHandler() throws WebException {
        WebObjectDescriptor desc = getDescriptor();
        if (desc != null) {
            return desc.getRequestHandler();
        }
        return null;
    }

    public String getPath() {
        return doc.getPathAsString();
    }

    public String getUrlPath() {
        if (urlPath == null) {
            PathInfo pi = context.getPathInfo();
            StringBuilder buf = new StringBuilder(context.getBasePath());
            if (pi.hasLeadingPath()) {
                buf.append(pi.getLeadingPath().toString());
            }
            if (pi.hasTraversalPath()) {
                buf.append(pi.getTraversalPath().toString());
            }
            urlPath = buf.toString();
        }
        return urlPath;
    }

    public DocumentModel traverse(String nextSegment) throws WebException {
        WebObjectDescriptor desc = getDescriptor();
        if (desc != null) {
            return desc.getRequestHandler().traverse(this, nextSegment);
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }

}
