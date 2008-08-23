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

import javax.ws.rs.GET;

import org.nuxeo.common.Environment;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.types.DefaultWebType;
import org.nuxeo.ecm.webengine.rest.types.WebType;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScriptObject extends WebObject {

    public final static WebType TYPE = new DefaultWebType("script", WebType.OBJECT);

    protected File file;

    @Override
    public void initialize(WebContext2 ctx, String path) {
        super.initialize(ctx, path);
        file = new File(Environment.getDefault().getWeb(), getDomain().descriptor.root);
        file = new File(file, path);
    }

    @Override
    public WebType getType() {
        return TYPE;
    }

    @GET
    public File get() {
        if (!file.exists()) {
            return null;
        } else {
            return file;
        }
    }

}
