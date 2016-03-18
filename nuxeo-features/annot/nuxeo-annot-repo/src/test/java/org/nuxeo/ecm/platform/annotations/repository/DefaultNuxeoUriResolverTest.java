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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author Alexandre Russel
 */
@LocalDeploy({ "org.nuxeo.ecm.platform.repository.test:OSGI-INF/other-repo.xml" })
public class DefaultNuxeoUriResolverTest extends AbstractRepositoryTestCase {

    private DefaultNuxeoUriResolver resolver;

    private CoreSession secondSession;

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
    public void testGetDocumentRef() {
        assertNotNull(uri);
        DocumentRef ref = resolver.getDocumentRef(uri);
        assertNotNull(ref);
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
        assertEquals(toStringURN(uri), graphUri.toString());
    }

    private String toStringURN(URI uri) {
        String[] uriSegments = uri.toString().split("/");
        int nbSegments = uriSegments.length;
        String ws = uriSegments[nbSegments - 2];
        String docId = uriSegments[nbSegments - 1];
        return String.format("urn:nuxeo:%s:%s", ws, docId);
    }

}
