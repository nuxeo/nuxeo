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

package org.nuxeo.ecm.webengine.model;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class View extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected String mediaType; 
    protected ObjectResource resource;
    protected Map<String,Object> args;
    protected ScriptFile template;
    

    public View(ObjectResource resource) {
        this (resource, null, null, null);
    }

    public View(ObjectResource resource, String name) {
        this (resource, name, null, null);
    }

    public View(ObjectResource resource, String name, String mediaType) {
        this (resource, name, mediaType, null);
    }

    public View(ObjectResource resource, String name, String mediaType, Map<String, Object> args) {
        this.resource = resource;
        this.name = name;
        this.mediaType = mediaType;
        this.args = args;
    }

    public View(ObjectResource resource, ScriptFile template) {
        this (resource, template, null);
    }

    public View(ObjectResource resource, ScriptFile template, Map<String,Object> args) {
        this.resource = resource;
        this.template = template;
        this.args = args;
    }

    public ScriptFile getTemplate() throws WebException {
        if (template == null) {
            template = resource.getContext().getApplication().getTemplate(resource, name, mediaType);
        }
        return template;        
    }
    
    public ObjectResource getResource() {
        return resource;
    }

    public WebContext getContext() {
        return resource.getContext();
    }
    
    public Map<String,Object> getArgs() {
        return args;
    }
    
    public void setArgs(Map<String,Object> args) {
        this.args = args;
    }
    
    /**
     * @return the name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * @param name the name to set.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * @return the mediaType.
     */
    public String getMediaType() {
        return mediaType;
    }
    
    /**
     * @param mediaType the mediaType to set.
     */
    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }
    

    public void render(OutputStream out) throws WebException {        
        Writer w = new OutputStreamWriter(out);
        try {
            resource.getContext().render(getTemplate(), args, w);
            w.flush();
        } catch (Exception e) {
            WebException.wrap("Failed to write response", e);
        }
    }

}

