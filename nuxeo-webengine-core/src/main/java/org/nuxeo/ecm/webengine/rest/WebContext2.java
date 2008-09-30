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

import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.model.WebAction;
import org.nuxeo.ecm.webengine.rest.model.WebApplication;
import org.nuxeo.ecm.webengine.rest.model.WebObject;
import org.nuxeo.ecm.webengine.rest.model.WebResource;
import org.nuxeo.ecm.webengine.rest.model.WebView;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.runtime.model.Adaptable;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface WebContext2 extends Adaptable {

    public void setApplication(WebApplication config);

    public WebApplication getApplication();

    public WebEngine2 getEngine();

    public UserSession getUserSession();

    public CoreSession getCoreSession();

    public Principal getPrincipal();

    WebObject newObject(String type, String path) throws WebException;
    
    WebAction newAction(String type, String name) throws WebException;

    UriInfo getUriInfo();
    
    /** object stack API */

    public WebResource push(String path, WebResource obj);

    public WebResource pop();

    public WebResource tail();

    public WebResource head();

    public WebObject getTargetObject();

    public WebAction getAction(); 

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

    public WebView getTemplate(String path) throws IOException;

    public WebView getTemplate(ScriptFile script);

}
