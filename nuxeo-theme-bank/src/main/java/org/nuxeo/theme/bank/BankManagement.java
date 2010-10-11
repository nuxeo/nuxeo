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

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.theme.resources.BankManager;

@WebObject(type = "BankManagement", administrator = Access.GRANT)
@Produces(MediaType.TEXT_HTML)
public class BankManagement extends DefaultObject {

    private static final Log log = LogFactory.getLog(BankManagement.class);

    String bank;

    @POST
    @Path("upload")
    public Object uploadFile() {
        FormData form = ctx.getForm();

        System.out.println(bank);
        String collection = form.getString("collection");
        String redirectUrl = form.getString("redirect_url");

        FileItem fileItem = form.getFileItem("file");
        if (!fileItem.isFormField()) {
            final byte[] fileData = fileItem.get();
            final String filename = fileItem.getName();
            final String path = String.format("%s/image/%s", bank, collection);
            BankManager.createFile(path, filename, fileData);
        }

        return redirect(redirectUrl);
    }

    @POST
    @Path("saveCss")
    public Object saveCss() {
        FormData form = ctx.getForm();

        String css = form.getString("css");
        String path = form.getString("path");
        String filename = form.getString("filename");

        BankManager.editFile(path, filename, css);

        String redirectUrl = form.getString("redirect_url");
        return redirect(redirectUrl);
    }

    @POST
    @Path("createStyle")
    public Object createStyle() {
        FormData form = ctx.getForm();

        String resource = form.getString("resource");
        String path = form.getString("path");
        String fileName = String.format("%s.css", resource);

        BankManager.createFile(path, fileName, "");

        String redirectUrl = form.getString("redirect_url");
        return redirect(redirectUrl);
    }
}
