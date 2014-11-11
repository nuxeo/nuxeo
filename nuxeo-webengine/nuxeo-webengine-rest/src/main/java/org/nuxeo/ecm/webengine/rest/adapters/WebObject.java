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

package org.nuxeo.ecm.webengine.rest.adapters;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.ws.rs.ProduceMime;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.actions.ActionDescriptor;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.domains.DefaultWebDomain;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.rest.types.WebType;
import org.nuxeo.runtime.model.Adaptable;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 *There is a bug in ResourceJavaMethodDispatcher#getAcceptableMediaType
 * when no setting the mime type it will return binary content ...
 */
@ProduceMime({"text/html", "*/*"})
public class WebObject implements Adaptable {

    protected WebContext2 ctx;
    protected String path;
    protected WebType type;

    public WebObject(WebType type) {
        this.type = type;
    }

    public void initialize(WebContext2 ctx, String path) {
        this.ctx = ctx;
        this.path = path;
        this.ctx.push(this);
    }

    public void setContext(WebContext2 ctx) {
        this.ctx = ctx;
    }

    public WebContext2 getContext() {
        return ctx;
    }

    public DefaultWebDomain<?> getDomain() {
        return ctx.getDomain();
    }

    /**
     * @return the path.
     */
    public String getPath() {
        return path;
    }

    public WebType getType() {
        return type;
    }

    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }


    public ScriptFile getActionScript(String action) {
        DefaultWebDomain<?> domain = ctx.getDomain();
        StringBuilder path = new StringBuilder();
        path.append('/').append(getType().getName()).append('/')
            .append(action).append('.').append(domain.getScriptExtension());
        try {
            return domain.getFile(path.toString());
        } catch (IOException e) {
            return null;
        }
    }

    public ScriptFile getTemplate(String action) {
        return getTemplateScript(action, null);
    }

    public ScriptFile getTemplateScript(String action, String format) {
        DefaultWebDomain<?> domain = ctx.getDomain();
        StringBuilder path = new StringBuilder();
        path.append('/').append(getType().getName()).append('/')
            .append(action).append('.');
        if (format != null) {
          path.append(format).append('.');
        }
        path.append(domain.getTemplateExtension());
        try {
            return domain.getFile(path.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public ActionDescriptor getAction(String action) {
        return null; //TODO
    }

    public Collection<ActionDescriptor> getActions() {
        return null; //TODO
    }

    public Collection<ActionDescriptor> getActions(String category) {
        return null; //TODO
    }

    public Map<String, Collection<ActionDescriptor>> getActionsByCategory() throws WebException {
        return null; //TODO
    }

}
