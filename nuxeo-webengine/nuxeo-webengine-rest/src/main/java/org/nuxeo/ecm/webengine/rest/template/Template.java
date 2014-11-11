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

package org.nuxeo.ecm.webengine.rest.template;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Template extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    protected WebContext2 ctx;
    protected ScriptFile script;

    public Template(WebContext2 ctx, ScriptFile script) {
        this.ctx = ctx;
        this.script = script;
    }

    public WebContext2 getContext() {
        return ctx;
    }

    public void render(OutputStream out) throws WebException {
        ctx.render(script, this, new OutputStreamWriter(out));
    }

}

