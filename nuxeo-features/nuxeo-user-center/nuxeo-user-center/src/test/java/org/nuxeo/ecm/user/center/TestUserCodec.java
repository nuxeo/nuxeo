/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.user.center;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.user.center.UserCodec.DEFAULT_USERS_TAB;
import static org.nuxeo.ecm.user.center.UserCodec.DEFAULT_VIEW_ID;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class TestUserCodec extends NXRuntimeTestCase {

    private DocumentView getDocumentView(String username, String view) {
        DocumentLocation docLoc = new DocumentLocationImpl("demo", null);
        Map<String, String> params = new HashMap<String, String>();
        params.put("username", username);
        return new DocumentViewImpl(docLoc, view, params);
    }

    @Test
    public void shouldGenerateAnURL() {
        UserCodec codec = new UserCodec();

        DocumentView docView = getDocumentView("bender", DEFAULT_VIEW_ID);
        String url = "user/bender/" + DEFAULT_VIEW_ID;
        assertEquals(url, codec.getUrlFromDocumentView(docView));

        docView = getDocumentView("leela", "view_user");
        url = "user/leela/" + "view_user";
        assertEquals(url, codec.getUrlFromDocumentView(docView));

        docView = getDocumentView(null, null);
        assertNull(codec.getUrlFromDocumentView(docView));

        docView = getDocumentView("fry", null);
        url = "user/fry";
        assertEquals(url, codec.getUrlFromDocumentView(docView));

        docView = getDocumentView("bender", null);
        docView.addParameter("tabIds", "FakeTab");
        url = "user/bender?tabIds=FakeTab";
        assertEquals(url, codec.getUrlFromDocumentView(docView));
    }

    @Test
    public void shouldGetDocumentView() throws Exception {
        deployContrib("org.nuxeo.ecm.user.center", "OSGI-INF/user-group-codec-properties.xml");
        UserCodec codec = new UserCodec();
        String url = "user/bender";
        DocumentView docView = codec.getDocumentViewFromUrl(url);
        assertNotNull(docView);
        assertEquals(DEFAULT_VIEW_ID, docView.getViewId());
        assertEquals("true", docView.getParameter("showUser"));
        assertEquals(DEFAULT_USERS_TAB, docView.getParameter("tabIds"));
        assertEquals("bender", docView.getParameter("username"));

        url = "user/bender/view_home";
        docView = codec.getDocumentViewFromUrl(url);
        assertNotNull(docView);
        assertEquals(DEFAULT_VIEW_ID, docView.getViewId());
        assertEquals("true", docView.getParameter("showUser"));
        assertEquals(DEFAULT_USERS_TAB, docView.getParameter("tabIds"));
        assertEquals("bender", docView.getParameter("username"));

        url = "user/leela?tabIds=USER_CENTER:UsersGroupsHome:UsersHome";
        docView = codec.getDocumentViewFromUrl(url);
        assertNotNull(docView);
        assertEquals(DEFAULT_VIEW_ID, docView.getViewId());
        assertEquals("true", docView.getParameter("showUser"));
        assertEquals(DEFAULT_USERS_TAB, docView.getParameter("tabIds"));
        assertEquals("leela", docView.getParameter("username"));

        url = "user/fry/view_user?tabIds=FakeTab";
        docView = codec.getDocumentViewFromUrl(url);
        assertNotNull(docView);
        assertEquals("view_user", docView.getViewId());
        assertEquals("true", docView.getParameter("showUser"));
        assertEquals("FakeTab", docView.getParameter("tabIds"));
        assertEquals("fry", docView.getParameter("username"));

        url = "user/zoidberg@planet-express.com/view_user";
        docView = codec.getDocumentViewFromUrl(url);
        assertNotNull(docView);
        assertEquals("view_user", docView.getViewId());
        assertEquals("true", docView.getParameter("showUser"));
        assertEquals(DEFAULT_USERS_TAB, docView.getParameter("tabIds"));
        assertEquals("zoidberg@planet-express.com", docView.getParameter("username"));

        deployContrib("org.nuxeo.ecm.user.center.tests", "OSGI-INF/test-user-group-codec-properties.xml");
        url = "user/bender%20bending";
        docView = codec.getDocumentViewFromUrl(url);
        assertNotNull(docView);
        assertEquals(DEFAULT_VIEW_ID, docView.getViewId());
        assertEquals("true", docView.getParameter("showUser"));
        assertEquals(DEFAULT_USERS_TAB, docView.getParameter("tabIds"));
        assertEquals("bender bending", docView.getParameter("username"));
    }

}
