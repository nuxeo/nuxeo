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

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.rest.scripting.Scripting;
import org.nuxeo.ecm.webengine.rest.types.WebType;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScriptObject extends WebObject {

    protected ScriptFile file;

    public ScriptObject(WebType type) {
        super (type);
    }

    @Override
    public void initialize(WebContext2 ctx, String path) {
        super.initialize(ctx, path);
        try {
            file = ctx.getDomain().getFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GET
    public Object get(@Context ServletContext servletCtx) throws WebException {
        if (file == null) {
            return null;
        } else {
            String ext = file.getExtension();
            Scripting scripting = ctx.getEngine().getScripting();
            if (file.isTemplate()) {
                return ctx.getTemplate(file);
            } else if (scripting.isScript(ext)) { // script
                return ctx.runScript(file, null);
            } else { // regular file
                File f = file.getFile();
                String ctype = servletCtx.getMimeType(f.getName());
                return Response.ok(f, ctype).build();
            }
        }
    }

}
