/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.theme.bank;

import java.io.File;
import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "nxthemes-bank")
@Produces("text/html")
public class Main extends ModuleRoot {

    private static final File BANKS_DIR;

    static {
        BANKS_DIR = new File(Framework.getRuntime().getHome(), "theme-banks");
        BANKS_DIR.mkdirs();
    }

    @GET
    public Object getIndex() {
        return getView("index");
    }

    @GET
    @Path("{bank}/style/{collection}/{resource}")
    public String getStyle(@PathParam("bank") String bank,
            @PathParam("collection") String collection,
            @PathParam("resource") String resource) {

        String path = String.format("%s/style/%s/%s", bank, collection, resource);
        File file = new File(BANKS_DIR, path);
        try {
            return FileUtils.readFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}