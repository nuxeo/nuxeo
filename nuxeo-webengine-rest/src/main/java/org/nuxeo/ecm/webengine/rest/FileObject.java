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

import javax.ws.rs.GET;

import org.nuxeo.common.Environment;
import org.nuxeo.ecm.webengine.rest.types.DefaultWebType;
import org.nuxeo.ecm.webengine.rest.types.WebType;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FileObject extends WebObject {

    public final static WebType TYPE = new DefaultWebType("file", WebType.OBJECT);

    /**
     *
     */
    public FileObject(String path) {
        super (path);
    }

    @Override
    public WebType getType() {
        return TYPE;
    }

    @GET
    public File get() {
        File file = Environment.getDefault().getWeb();
        return new File(file, path);
    }

}
