/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model;

import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.webengine.scripting.ScriptFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class View extends Template {

    protected String name;

    protected String ext;

    protected MediaType mediaType;

    protected View(WebContext ctx, Resource resource, String name) {
        super(ctx, resource, null);
        mediaType = ctx.getHttpHeaders().getMediaType();
        this.name = name;
    }

    public View(WebContext ctx, String name) {
        this(ctx, null, name);
    }

    public View(Resource resource, String name) {
        this(resource.getContext(), resource, name);
    }

    public View mediaType(MediaType mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    public View name(String name) {
        this.name = name;
        return this;
    }

    public View extension(String ext) {
        this.ext = ext;
        return this;
    }

    public MediaType mediaType() {
        return mediaType;
    }

    public View fileName(String fileName) {
        resolve(fileName);
        return this;
    }

    public View script(ScriptFile script) {
        this.script = script;
        return this;
    }

    @Override
    public ScriptFile script() {
        if (script == null) {
            StringBuilder fileName = new StringBuilder();
            fileName.append(name);
            if (mediaType != null) {
                String mediaId = ctx.getModule().getMediaTypeId(mediaType);
                if (mediaId != null) {
                    fileName.append('-').append(mediaId);
                }
            }
            if (ext == null) {
                ext = ctx.getModule().getTemplateFileExt();
            }
            fileName.append('.').append(ext);
            resolve(fileName.toString());
        }
        return script;
    }

    public View resolve() {
        script(); // resolve script
        return this;
    }

}
