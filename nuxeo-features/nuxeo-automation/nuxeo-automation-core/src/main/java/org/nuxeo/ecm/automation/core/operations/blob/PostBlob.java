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
 */
package org.nuxeo.ecm.automation.core.operations.blob;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;

/**
 * Save the input document
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = PostBlob.ID, category = Constants.CAT_BLOB, label = "HTTP Post", description = "Post the input file to a target HTTP URL. Returns back the input file.")
public class PostBlob {

    public static final String ID = "Blob.Post";

    @Param(name = "url")
    protected String url;

    @OperationMethod
    public Blob run(Blob blob) throws Exception {
        URL target = new URL(url);
        URLConnection conn = target.openConnection();
        conn.setDoOutput(true);
        InputStream in = blob.getStream();
        OutputStream out = conn.getOutputStream();
        try {
            FileUtils.copy(in, out);
            out.flush();
        } finally {
            in.close();
            out.close();
        }
        return blob;
    }

}
