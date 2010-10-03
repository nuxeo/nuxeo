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

@WebObject(type = "Management", administrator = Access.GRANT)
@Produces(MediaType.TEXT_HTML)
public class Management extends DefaultObject {

    private static final Log log = LogFactory.getLog(Management.class);

    @POST
    @Path("upload")
    public Object uploadFile() {
        FormData form = ctx.getForm();

        String bankName = form.getString("bank");
        String collection = form.getString("collection");
        String redirectUrl = form.getString("redirect_url");

        FileItem fileItem = form.getFileItem("file");
        if (!fileItem.isFormField()) {
            final byte[] fileData = fileItem.get();
            final String filename = fileItem.getName();

            String path = String.format("%s/image/%s", bankName, collection);
            BankManager.createFile(path, filename, fileData);
        }

        return redirect(redirectUrl);
    }

}
