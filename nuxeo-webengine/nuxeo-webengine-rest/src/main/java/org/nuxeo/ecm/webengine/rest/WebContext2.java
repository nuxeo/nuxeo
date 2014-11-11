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

package org.nuxeo.ecm.webengine.rest;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.security.Principal;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.adapters.WebObject;
import org.nuxeo.ecm.webengine.rest.domains.DefaultWebDomain;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.rest.template.Template;
import org.nuxeo.ecm.webengine.session.UserSession;

import com.sun.jersey.api.core.HttpContext;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface WebContext2 {

    public void setDomain(DefaultWebDomain<?> domain);

    public DefaultWebDomain<?> getDomain();

    public WebEngine2 getEngine();

    public UserSession getUserSession();

    public CoreSession getCoreSession();

    public Principal getPrincipal();

    public HttpContext getHttpContext();

    public void setAction(String action);

    public String getAction();


    /** object stack API */

    public void push(WebObject obj);

    public WebObject pop();

    public WebObject tail();

    public WebObject head();



    /** template and script resolver */

    public ScriptFile getFile(String path) throws IOException;

    public void pushScriptFile(File file);

    public File popScriptFile();



    /** running scripts and rendering templates */

    public void render(String template, Writer writer) throws WebException;

    public void render(String template, Object ctx, Writer writer) throws WebException;

    @SuppressWarnings("unchecked")
    public void render(ScriptFile script, Object ctx, Writer writer) throws WebException;

    public Object runScript(String script) throws WebException;

    public Object runScript(String script, Map<String, Object> args) throws WebException;

    public Object runScript(ScriptFile script, Map<String, Object> args) throws WebException;

    public Template getTemplate(String path) throws IOException;

    public Template getTemplate(ScriptFile script);

}
