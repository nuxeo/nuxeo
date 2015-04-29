/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;

import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy({ "org.nuxeo.ecm.platform.convert" })
public class ConverterTest extends BaseTest {

    @Test
    public void shouldConvertBlobUsingNamedConverter() {
        DocumentModel doc = createDummyDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("converter", "any2pdf");
        ClientResponse response = getResponse(RequestType.GET, "path" + doc.getPathAsString()
                + "/@blob/file:content/@convert", queryParams);
        assertEquals(200, response.getStatus());
    }

    protected DocumentModel createDummyDocument() {
        DocumentModel doc = session.createDocumentModel("/", "adoc", "File");
        Blob blob = Blobs.createBlob("Dummy txt", "text/plain", null, "dummy.txt");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        return doc;
    }

    @Test
    public void shouldConvertBlobUsingMimeType() {
        DocumentModel doc = createDummyDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("type", "application/pdf");
        ClientResponse response = getResponse(RequestType.GET, "path" + doc.getPathAsString()
                + "/@blob/file:content/@convert", queryParams);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void shouldConvertBlobUsingFormat() {
        DocumentModel doc = createDummyDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("format", "pdf");
        ClientResponse response = getResponse(RequestType.GET, "path" + doc.getPathAsString()
                + "/@blob/file:content/@convert", queryParams);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void shouldConvertDocument() {
        DocumentModel doc = createDummyDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("converter", "any2pdf");
        ClientResponse response = getResponse(RequestType.GET, "path" + doc.getPathAsString() + "/@convert",
                queryParams);
        assertEquals(200, response.getStatus());
    }

}
