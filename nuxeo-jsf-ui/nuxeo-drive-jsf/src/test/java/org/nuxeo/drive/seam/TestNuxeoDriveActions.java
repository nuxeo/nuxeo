/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.seam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collections;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.mock.MockApplication;
import org.jboss.seam.mock.MockExternalContext;
import org.jboss.seam.mock.MockFacesContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.transientstore.keyvalueblob.KeyValueBlobTransientStoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests the {@link NuxeoDriveActions}.
 *
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ KeyValueBlobTransientStoreFeature.class, PlatformFeature.class })
@Deploy("org.nuxeo.ecm.platform.web.common")
@Deploy("org.nuxeo.ecm.automation.server:OSGI-INF/auth-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.login.token")
public class TestNuxeoDriveActions {

    @Inject
    protected CoreSession session;

    @Inject
    protected TokenAuthenticationService tokenAuthenticationService;

    protected NuxeoDriveActions nuxeoDriveActions;

    protected NuxeoPrincipal principal;

    @Before
    public void setUp() {
        nuxeoDriveActions = new MockNuxeoDriveActions(session);
        principal = session.getPrincipal();
    }

    @Test
    public void testHasOneDriveToken() throws UnsupportedEncodingException {
        assertFalse(nuxeoDriveActions.hasOneDriveToken(principal));

        // Acquire a non Nuxeo Drive token
        String token = acquireToken("myApp");
        assertFalse(nuxeoDriveActions.hasOneDriveToken(principal));
        revokeToken(token);

        // Acquire a Nuxeo Drive token with applicationName not encoded
        token = acquireToken("Nuxeo Drive");
        assertTrue(nuxeoDriveActions.hasOneDriveToken(principal));
        revokeToken(token);

        // Acquire a Nuxeo Drive token with applicationName percent encoded (testing backward compatibility)
        token = acquireToken("Nuxeo%20Drive");
        assertTrue(nuxeoDriveActions.hasOneDriveToken(principal));
        revokeToken(token);

        // Acquire a Nuxeo Drive token with applicationName form-urlencoded (testing backward compatibility)
        token = acquireToken("Nuxeo+Drive");
        assertTrue(nuxeoDriveActions.hasOneDriveToken(principal));
        revokeToken(token);
    }

    /**
     * In the code we are testing, we have the following instructions to retrieve the current server URL:
     *
     * <pre>
     * ServletRequest servletRequest = (ServletRequest) FacesContext.getCurrentInstance()
     *                                                              .getExternalContext()
     *                                                              .getRequest();
     *
     * String baseURL = VirtualHostHelper.getBaseURL(servletRequest).replaceFirst("://", "/");
     * </pre>
     *
     * While testing, there is no relevant context, which results in a {@link NullPointerException}. To prevent this, we
     * are mocking the objects so that the execution of this snippet results in having the correct baseURL.
     */
    @Test
    public void testGetDriveEditURL() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getScheme()).thenReturn("http");

        MockExternalContext externalContext = new MockExternalContext();
        externalContext.setRequest(request);
        MockFacesContext facesContext = new MockFacesContext(externalContext, new MockApplication());
        facesContext.setCurrent();

        session.createDocument(session.createDocumentModel("/", "foo", "Folder"));
        DocumentModel doc = session.createDocumentModel("/foo", "bar", "File");

        Blob mainBlob = new StringBlob("bla bla bla");
        mainBlob.setFilename("bla.odt");
        Blob secondBlob = new StringBlob("bli bli bli");
        secondBlob.setFilename("bli.odt");

        doc.setPropertyValue("file:content", (Serializable) mainBlob);
        doc.setPropertyValue("files:files",
                (Serializable) Collections.singletonList(Collections.singletonMap("file", secondBlob)));
        doc = session.createDocument(doc);

        String mainBlobURL = nuxeoDriveActions.getDriveEditURL(doc);
        String expectedMainBlobURL = String.format(
                "nxdrive://edit/http/localhost:8080/nuxeo/user/Administrator/repo/test/nxdocid/%s/filename/bla.odt/downloadUrl/nxfile/test/%s/blobholder:0/bla.odt",
                doc.getId(), doc.getId());
        assertEquals(expectedMainBlobURL, mainBlobURL);

        String secondBlobURL = nuxeoDriveActions.getDriveEditURL(doc, "files:files/0/file");
        String expectedSecondBlobURL = String.format(
                "nxdrive://edit/http/localhost:8080/nuxeo/user/Administrator/repo/test/nxdocid/%s/filename/bli.odt/downloadUrl/nxfile/test/%s/files:files/0/file/bli.odt",
                doc.getId(), doc.getId());
        assertEquals(expectedSecondBlobURL, secondBlobURL);

        try {
            nuxeoDriveActions.getDriveEditURL(doc, "files:files/1/file");
            fail("There is no blob at this xPath");
        } catch (PropertyNotFoundException e) {
            // Nothing to do here
        } catch (Exception e) {
            fail("Exception should be a PropertyNotFoundException instead of " + e);
        }

        try {
            nuxeoDriveActions.getDriveEditURL(doc, null);
            fail("The xPath should not be null");
        } catch (PropertyNotFoundException e) {
            // Nothing to do here
        } catch (Exception e) {
            fail("Exception should be a PropertyNotFoundException instead of " + e);
        }
    }

    protected String acquireToken(String applicationName) {
        return tokenAuthenticationService.acquireToken("Administrator", applicationName, "myDeviceId",
                "myDeviceDescription", "rw");
    }

    protected void revokeToken(String token) {
        tokenAuthenticationService.revokeToken(token);
    }

}
