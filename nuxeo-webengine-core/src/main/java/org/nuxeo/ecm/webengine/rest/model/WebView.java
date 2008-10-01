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

package org.nuxeo.ecm.webengine.rest.model;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebView extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    protected Resource resource;
    protected ScriptFile script;
    protected Map<String,Object> args;

//    public WebView(Resource resource) {
//        this (resource, findScript(resource), null);
//    }

    public WebView(Resource resource, ScriptFile script) {
        this (resource, script, null);
    }

    public WebView(Resource resource, ScriptFile script, Map<String,Object> args) {
        this.resource = resource;
        this.script = script;
        this.args = args;
    }

    public Resource getResource() {
        return resource;
    }

    public WebContext2 getContext() {
        return resource.getContext();
    }
    
    public Map<String,Object> getArgs() {
        return args;
    }
    
    public void setArgs(Map<String,Object> args) {
        this.args = args;
    }

    public void render(OutputStream out) throws WebException {
        Writer w = new OutputStreamWriter(out);
        try {
            resource.getContext().render(script, args, w);
            w.flush();
        } catch (Exception e) {
            WebException.wrap("Failed to write response", e);
        }
    }

}

