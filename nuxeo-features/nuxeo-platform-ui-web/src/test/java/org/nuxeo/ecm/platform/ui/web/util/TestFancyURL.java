/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.util;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.rest.FancyURLConfig;
import org.nuxeo.ecm.platform.ui.web.rest.FancyURLMapper;
import org.nuxeo.ecm.platform.url.api.DocumentView;

public class TestFancyURL extends TestCase {

    private static final Log log = LogFactory.getLog(TestFancyURL.class);

    public void testExtractParametersFromTraversal() {
        String url1 = FancyURLConfig.FANCY_URL_PREFIX
                + "/server1/d3c36ab5-56c8-45e4-a587-368dd7c91473/view_documents/action.id";

        DocumentView docView = FancyURLMapper.extractParametersFromURL(url1);

        assertNotNull(docView);

        assertEquals("server1",
                docView.getDocumentLocation().getServerLocationName());
        assertEquals("d3c36ab5-56c8-45e4-a587-368dd7c91473",
                docView.getDocumentLocation().getDocRef().toString());
        assertEquals("view_documents", docView.getViewId());
        assertEquals("action.id", docView.getTabId());

        url1 += "/";

        docView = FancyURLMapper.extractParametersFromURL(url1);

        assertNotNull(docView);

        assertEquals("server1",
                docView.getDocumentLocation().getServerLocationName());
        assertEquals("d3c36ab5-56c8-45e4-a587-368dd7c91473",
                docView.getDocumentLocation().getDocRef().toString());
        assertEquals("view_documents", docView.getViewId());
        assertEquals("action.id", docView.getTabId());
    }

    public void testGenerateURL() {
        String url1 = FancyURLConfig.FANCY_URL_PREFIX
                + "/server1/d3c36ab5-56c8-45e4-a587-368dd7c91473/view_documents/action.id";

        DocumentView docView = FancyURLMapper.extractParametersFromURL(url1);

        assertNotNull(docView);

        url1 = FancyURLConfig.FANCY_URL_PREFIX
                + "/server1/d3c36ab5-56c8-45e4-a587-368dd7c91473/view_documents/action.id/";

        docView = FancyURLMapper.extractParametersFromURL(url1);

        assertNotNull(docView);

        String url2 = FancyURLMapper.getFancyURL(docView);

        assertEquals(url1, url2);
    }

    public void testResources() {
        String url = FancyURLConfig.FANCY_URL_PREFIX
                + "/demo/b5b372af-46f2-406d-8479-87b8a9c3fa01/view_documents/action.id";

        DocumentView docView = FancyURLMapper.extractParametersFromURL(url);

        assertNull(docView.getSubURI());

        String url1 = FancyURLConfig.FANCY_URL_PREFIX
                + "/demo/b5b372af-46f2-406d-8479-87b8a9c3fa01/view_documents/action.id/waitdialog/waitdlg.js";

        docView = FancyURLMapper.extractParametersFromURL(url1);

        assertEquals("waitdialog/waitdlg.js", docView.getSubURI());
    }

    public void testRedirectRewriting() {
        String url = "http://127.0.0.1:8080/nuxeo/view_documents.faces?currentTab=TAB_CONTENT&documentId=d3c36ab5-56c8-45e4-a587-368dd7c91473&repositoryName=demo&conversationId=0NXMAIN&conversationIsLongRunning=true";

        String url2 = FancyURLMapper.convertToFancyURL(url);

        assertEquals(
                url2,
                "http://127.0.0.1:8080/nuxeo"
                        + FancyURLConfig.FANCY_URL_PREFIX
                        + "/demo/d3c36ab5-56c8-45e4-a587-368dd7c91473/view_documents/TAB_CONTENT/?conversationId=0NXMAIN&conversationIsLongRunning=true");
    }

}
