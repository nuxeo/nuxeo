/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @author Alexandre Russel
 */
@Deploy({ "org.nuxeo.ecm.annotations.repository.test:OSGI-INF/other-repo.xml" })
public class DefaultNuxeoUriResolverTest extends AbstractRepositoryTestCase {

    private DefaultNuxeoUriResolver resolver;

    private CloseableCoreSession secondSession;

    private URI uriSecondRepo;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setUpRepository();
        resolver = new DefaultNuxeoUriResolver();
        secondSession = CoreInstance.openCoreSession("second", session.getPrincipal());
        uriSecondRepo = setUpRepository(secondSession);
    }

    @After
    public void tearDown() {
        secondSession.close();
    }

    @Test
    public void testGetDocumentRefWithURI() {
        testGetDocumentRef(uri);
    }

    @Test
    public void testGetDocumentRefWithURN() {
        testGetDocumentRef(toURN(uri));
    }

    @Test
    public void testGetDocumentRefWithURIAndSecondRepo() {
        testGetDocumentRef(uriSecondRepo);
    }

    @Test
    public void testGetDocumentRefWithURNAndSecondRepo() {
        testGetDocumentRef(toURN(uriSecondRepo));
    }

    private void testGetDocumentRef(URI uri) {
        assertNotNull(uri);
        DocumentRef ref = resolver.getDocumentRef(uri);
        assertNotNull(ref);
        if (ref instanceof IdRef) {
            String uriString = uri.toString();
            char separator;
            if (uriString.startsWith("urn:")) {
                separator = ':';
            } else {
                separator = '/';
            }
            assertEquals(uriString.substring(uriString.lastIndexOf(separator) + 1), ref.reference());
        } else if (ref instanceof PathRef) {
            assertEquals("/1", ref.reference());
        }
    }

    @Test
    public void testTranslateToGraphURI() {
        testTranslateToGraphURI(uri);
    }

    @Test
    public void testTranslateToGraphURIWithSecondRepo() {
        testTranslateToGraphURI(uriSecondRepo);
    }

    private void testTranslateToGraphURI(URI uri) {
        assertNotNull(uri);
        URI graphUri = resolver.translateToGraphURI(uri);
        assertGraphURI(uri, graphUri);
    }

    @Test
    public void testGetGraphURIFromDocumentView() {
        testGetGraphURIFromDocumentView(session, uri);
    }

    @Test
    public void testGetGraphURIFromDocumentViewWithSecondRepo() {
        testGetGraphURIFromDocumentView(secondSession, uriSecondRepo);
    }

    private void testGetGraphURIFromDocumentView(CoreSession session, URI uri) {
        assertNotNull(uri);
        // In order to cover NXP-19146 build DocumentView with a PathRef
        PathRef pathRef = new PathRef("/1");
        DocumentView docView = new DocumentViewImpl(new DocumentLocationImpl(session.getRepositoryName(), pathRef));
        // Test
        URI graphUri = resolver.getGraphURIFromDocumentView(docView);
        assertGraphURI(uri, graphUri);
    }

    private void assertGraphURI(URI uri, URI graphUri) {
        assertNotNull(graphUri);
        String[] uriSegments = uri.toString().split("/");
        int nbSegments = uriSegments.length;
        String ws = uriSegments[nbSegments - 2];
        String docId = uriSegments[nbSegments - 1];
        assertEquals(String.format("urn:nuxeo:%s:%s", ws, docId), graphUri.toString());
    }

    private URI toURN(URI uri) {
        try {
            return new URI(toStringURN(uri));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String toStringURN(URI uri) {
        String[] uriSegments = uri.toString().split("/");
        int nbSegments = uriSegments.length;
        String ws = uriSegments[nbSegments - 2];
        String docId = uriSegments[nbSegments - 1];
        return String.format("urn:nuxeo:%s:%s", ws, docId);
    }

}
