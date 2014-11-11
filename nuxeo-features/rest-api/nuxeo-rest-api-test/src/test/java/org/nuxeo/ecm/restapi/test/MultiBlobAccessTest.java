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

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.StreamDataBodyPart;

/**
 *
 *
 * @since 5.8
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@LocalDeploy("org.nuxeo.ecm.platform.restapi.test:multiblob-doctype.xml")
public class MultiBlobAccessTest extends BaseTest {

    @Inject
    CoreSession session;

    private DocumentModel doc;

    @Override
    @Before
    public void doBefore() throws Exception {
        super.doBefore();
        doc = session.createDocumentModel("/", "testBlob", "MultiBlobDoc");
        addBlob(doc, new StringBlob("one"));
        addBlob(doc, new StringBlob("two"));
        doc = session.createDocument(doc);
        session.save();
    }

    @Test
    public void itCanAccessBlobs() throws Exception {

        // When i call the rest api
        ClientResponse response = getResponse(RequestType.GET,
                "path" + doc.getPathAsString() + "/@blob/mb:blobs/0/content");

        // Then i receive the content of the blob
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("one", response.getEntity(String.class));

        response = getResponse(RequestType.GET, "path" + doc.getPathAsString()
                + "/@blob/mb:blobs/1/content");

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("two", response.getEntity(String.class));

    }

    @Test
    public void itCanModifyABlob() throws Exception {
        // Given a doc with a blob

        // When i send a PUT with a new value on the blob
        FormDataMultiPart form = new FormDataMultiPart();
        BodyPart fdp = new StreamDataBodyPart("content",
                new ByteArrayInputStream("modifiedData".getBytes()));
        form.bodyPart(fdp);
        getResponse(RequestType.PUT, "path" + doc.getPathAsString()
                + "/@blob/mb:blobs/0/content", form);
        form.close();

        // The the blob is updated
        fetchInvalidations();
        doc = getTestBlob();
        Blob blob = (Blob) doc.getPropertyValue("mb:blobs/0/content");
        StringWriter sw = new StringWriter();
        blob.transferTo(sw);
        assertEquals("modifiedData", sw.toString());

    }

    @Test
    public void itCanRemoveABlob() throws Exception {
        // Given a doc with a blob

        // When i send A DELETE command on its blob
        getResponse(RequestType.DELETE, "path" + doc.getPathAsString()
                + "/@blob/mb:blobs/0/content");

        // The the blob is reset
        fetchInvalidations();
        doc = getTestBlob();
        Blob blob = (Blob) doc.getPropertyValue("mb:blobs/0/content");
        assertNull(blob);
    }

    private DocumentModel getTestBlob() throws ClientException {
        return session.getDocument(new PathRef("/testBlob"));
    }

    /**
     * @param doc
     * @param stringBlob
     * @throws ClientException
     *
     */
    private void addBlob(DocumentModel doc, Blob blob) throws ClientException {
        Map<String, Serializable> blobProp = new HashMap<>();
        blobProp.put("content", (Serializable) blob);
        List<Map<String, Serializable>> blobs = (List<Map<String, Serializable>>) doc.getPropertyValue("mb:blobs");
        blobs.add(blobProp);
        doc.setPropertyValue("mb:blobs", (Serializable) blobs);
    }
}
