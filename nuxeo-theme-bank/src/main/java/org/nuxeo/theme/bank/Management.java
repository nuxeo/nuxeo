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
            final String path = String.format("%s/image/%s", bank, collection);
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

        final String path = String.format("%s/style/%s", bank, collection);

        try {
            BankManager.editFile(path, resource, css);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }

        String redirectUrl = form.getString("redirect_url");
        return redirect(redirectUrl);
    }

    @POST
    @Path("createStyle")
    public Object createStyle() {
        FormData form = ctx.getForm();

        String resource = form.getString("resource");
        String collection = form.getString("collection");
        final String path = String.format("%s/style/%s", bank, collection);
        String fileName = String.format("%s.css", resource);

        try {
            BankManager.createFile(path, fileName, "");
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }

        String redirectUrl = form.getString("redirect_url");
        return redirect(redirectUrl);
    }
}
