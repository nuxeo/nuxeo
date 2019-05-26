/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.util.RepositoryLocation;

public class TestDocumentLocator {

    private static final Log log = LogFactory.getLog(TestDocumentLocator.class);

    @Test
    public void testDocUrlCreation() {
        DocumentRef docRef = new IdRef("xxxx-xxx-xxxxx-xxxxxxxxx-xx");
        RepositoryLocation nullServer = null;
        final String result = DocumentLocator.getDocumentUrl(nullServer, docRef);

        log.info("result: " + result);
    }

    @Test
    public void testDocUrlCreation2() {
        RepositoryLocation rep = new RepositoryLocation("alpha");
        DocumentRef docRef = new IdRef("xxxx-xxx-xxxxx-xxxxxxxxx-xx");
        final String result = DocumentLocator.getDocumentUrl(rep, docRef);

        log.info("result: " + result);

        try {
            assertNotNull(new URI(result));
        } catch (URISyntaxException e) {
            fail("not a valid result: " + e.getMessage());
        }
    }

    @Test
    public void testDocFullUrlCreation() {
        RepositoryLocation rep = new RepositoryLocation("alpha");
        DocumentRef docRef = new IdRef("xxxx-xxx-xxxxx-xxxxxxxxx-xx");
        // XXX AT: no context so server is not found => add info
        final String result = "http://localhost:8080/nuxeo/" + DocumentLocator.getFullDocumentUrl(rep, docRef);
        log.info("result: " + result);

        try {
            assertNotNull(new URL(result));
        } catch (MalformedURLException e) {
            fail("not a valid result: " + e.getMessage());
        }
    }
}
