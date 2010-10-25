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

import java.io.IOException;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.theme.resources.BankManager;

@WebObject(type = "Management", administrator = Access.GRANT)
@Produces(MediaType.TEXT_HTML)
public class Management extends DefaultObject {

    private static final Log log = LogFactory.getLog(Management.class);

    String bank;

    @Override
    protected void initialize(Object... args) {
        assert args != null && args.length > 0;
        bank = (String) args[0];
    }

    @POST
    @Path("upload")
    public Object uploadFile() {
        FormData form = ctx.getForm();

        String collection = form.getString("collection");
        String redirectUrl = form.getString("redirect_url");

        FileItem fileItem = form.getFileItem("file");
        if (!fileItem.isFormField()) {
            final byte[] fileData = fileItem.get();
            final String filename = fileItem.getName();
            final String path = String.format("%s/%s/image", bank, collection);
            try {
                BankManager.createFile(path, filename, fileData);
            } catch (IOException e) {
                throw new ThemeBankException(e.getMessage(), e);
            }
        }
        if (redirectUrl != null) {
            return redirect(redirectUrl);
        } else {
            return null;
        }
    }

    @POST
    @Path("saveCss")
    public Object saveCss() {
        FormData form = ctx.getForm();

        String css = form.getString("css");
        String collection = form.getString("collection");
        String resource = form.getString("resource");

        final String path = String.format("%s/%s/style", bank, collection);

        try {
            BankManager.editFile(path, resource, css);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }

        String redirectUrl = form.getString("redirect_url");
        return redirect(redirectUrl);
    }

    @POST
    @Path("{collection}/createStyle")
    public Object createStyle(@PathParam("collection") String collection) {
        FormData form = ctx.getForm();

        String resource = form.getString("resource");
        final String path = String.format("%s/%s/style", bank, collection);
        String fileName = String.format("%s.css", resource);

        try {
            BankManager.createFile(path, fileName, "");
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }

        String redirectUrl = form.getString("redirect_url");
        return redirect(redirectUrl);
    }

    @POST
    @Path("{collection}/download")
    public Response downloadCollection(
            @PathParam("collection") String collection) {
        byte[] data;
        try {
            data = BankManager.exportBankData(bank, collection);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
        String filename = String.format("%s.zip", collection.replace(" ", "-"));
        ResponseBuilder builder = Response.ok(data);
        builder.header("Content-disposition",
                String.format("attachment; filename=%s", filename));
        builder.type("application/zip");
        return builder.build();
    }
}
