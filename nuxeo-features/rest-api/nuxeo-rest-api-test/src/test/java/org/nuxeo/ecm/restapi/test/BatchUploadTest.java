/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.StreamDataBodyPart;

/**
 * @since 5.8
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@LocalDeploy("org.nuxeo.ecm.platform.restapi.test:multiblob-doctype.xml")
public class BatchUploadTest extends BaseTest {

    @Inject
    CoreSession session;

    @Test
    public void itCanUseBatchUpload() throws Exception {
        String batchId = "batch_" + Math.random();
        String filename = "testfile";
        String data = "batchUploadedData";

        // upload the file in automation
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-Batch-Id", batchId);
        headers.put("X-File-Idx", "0");
        headers.put("X-File-Name", filename);
        FormDataMultiPart form = new FormDataMultiPart();
        BodyPart fdp = new StreamDataBodyPart(filename, new ByteArrayInputStream(data.getBytes()));
        form.bodyPart(fdp);
        getResponse(RequestType.POST, "automation/batch/upload", form, headers);
        form.close();

        // create the doc which references the given blob
        String json = "{";
        json += "\"entity-type\":\"document\" ,";
        json += "\"name\":\"testBatchUploadDoc\" ,";
        json += "\"type\":\"MultiBlobDoc\" ,";
        json += "\"properties\" : {";
        json += "\"mb:blobs\" : [ ";
        json += "{ \"filename\" : \"" + filename + "\" , \"content\" : { \"upload-batch\": \"" + batchId
                + "\", \"upload-fileId\": \"0\" } }";
        json += "]}}";
        ClientResponse response = getResponse(RequestType.POST, "path/", json);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        DocumentModel doc = session.getDocument(new PathRef("/testBatchUploadDoc"));
        Blob blob = (Blob) doc.getPropertyValue("mb:blobs/0/content");
        assertNotNull(blob);
        assertEquals(data, new String(blob.getByteArray()));
    }

}
