/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.blob;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = PostBlob.ID, category = Constants.CAT_BLOB, label = "HTTP Post", description = "Post the input file to a target HTTP URL. Returns back the input file.", aliases = {
        "Blob.Post" })
public class PostBlob {

    public static final String ID = "Blob.PostToURL";

    @Param(name = "url")
    protected String url;

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob blob) throws IOException {
        URL target = new URL(url);
        URLConnection conn = target.openConnection();
        conn.setDoOutput(true);
        try (InputStream in = blob.getStream(); OutputStream out = conn.getOutputStream()) {
            IOUtils.copy(in, out);
            out.flush();
        }
        return blob;
    }

}
