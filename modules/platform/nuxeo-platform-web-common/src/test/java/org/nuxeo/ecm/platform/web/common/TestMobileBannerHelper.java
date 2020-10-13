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
 *     Thomas Roger
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.platform.web.common;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.url")
public class TestMobileBannerHelper {

    public static final String BASE_URL = "http://localhost:8080/nuxeo/";

    public static final String OTHER_BASE_URL = "https://demo.nuxeo.com/nuxeo";

    @Inject
    protected CoreSession session;

    protected DocumentModel doc;

    @Before
    public void init() {
        Framework.getProperties().put("nuxeo.mobile.application.protocol", "nuxeo");
        Framework.getProperties().put("nuxeo.mobile.application.android.package", "com.nuxeomobile");
        Framework.getProperties().put("nuxeo.mobile.application.iTunesId", "id1103802613");
        doc = session.createDocument(session.createDocumentModel("/", "testDoc", "File"));
    }

    @Test
    public void testProtocols() {
        assertEquals("nuxeo://", MobileBannerHelper.getIOSProtocol());
        assertEquals("android-app://com.nuxeomobile/nuxeo/", MobileBannerHelper.getAndroidProtocol());
    }

    @Test
    public void testAppStoreURL() {
        assertEquals("https://itunes.apple.com/app/id1103802613", MobileBannerHelper.getAppStoreURL());
    }

    @Test
    public void testURLForMobileApplications() {
        String protocol = MobileBannerHelper.getIOSProtocol();

        String url = MobileBannerHelper.getURLForMobileApplication(protocol, BASE_URL, doc, null);
        assertEquals("nuxeo://http/localhost:8080/nuxeo/test/id/" + doc.getId(), url);
        url = MobileBannerHelper.getURLForMobileApplication(protocol, OTHER_BASE_URL, doc, null);
        assertEquals("nuxeo://https/demo.nuxeo.com/nuxeo/test/id/" + doc.getId(), url);

        String requestedURL = "nxdoc/default/abd6e1a0-0a4d-4654-8ca2-92480b7f3d1b/view_documents";
        url = MobileBannerHelper.getURLForMobileApplication(protocol, BASE_URL, null, requestedURL);
        assertEquals("nuxeo://http/localhost:8080/nuxeo/default/id/abd6e1a0-0a4d-4654-8ca2-92480b7f3d1b", url);
        url = MobileBannerHelper.getURLForMobileApplication(protocol, OTHER_BASE_URL, null, requestedURL);
        assertEquals("nuxeo://https/demo.nuxeo.com/nuxeo/default/id/abd6e1a0-0a4d-4654-8ca2-92480b7f3d1b", url);

        requestedURL = "nxpath/default/default-domain/workspaces/foo@view_documents";
        url = MobileBannerHelper.getURLForMobileApplication(protocol, BASE_URL, null, requestedURL);
        assertEquals("nuxeo://http/localhost:8080/nuxeo/default/path/default-domain/workspaces/foo", url);
        url = MobileBannerHelper.getURLForMobileApplication(protocol, OTHER_BASE_URL, null, requestedURL);
        assertEquals("nuxeo://https/demo.nuxeo.com/nuxeo/default/path/default-domain/workspaces/foo", url);

        requestedURL = "nxdoc/default/";
        url = MobileBannerHelper.getURLForMobileApplication(protocol, BASE_URL, null, requestedURL);
        assertEquals("nuxeo://http/localhost:8080/nuxeo/", url);

        requestedURL = "nxhome/not/exist";
        url = MobileBannerHelper.getURLForMobileApplication(protocol, OTHER_BASE_URL, null, requestedURL);
        assertEquals("nuxeo://https/demo.nuxeo.com/nuxeo/", url);
    }
}
