/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.ui.web.auth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;

/**
 * @since 8.10
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.url.core" })
public class TestLoginScreenHelper {

    public static final String BASE_URL = "http://localhost:8080/nuxeo/";

    public static final String OTHER_BASE_URL = "https://demo.nuxeo.com/nuxeo";

    @Test
    public void testURLForMobileApplications() {
        String requestedURL = "nxdoc/default/abd6e1a0-0a4d-4654-8ca2-92480b7f3d1b/view_documents";
        String url = LoginScreenHelper.getURLForMobileApplication(BASE_URL, requestedURL);
        assertEquals("nuxeo://http/localhost:8080/nuxeo/default/id/abd6e1a0-0a4d-4654-8ca2-92480b7f3d1b", url);
        url = LoginScreenHelper.getURLForMobileApplication(OTHER_BASE_URL, requestedURL);
        assertEquals("nuxeo://https/demo.nuxeo.com/nuxeo/default/id/abd6e1a0-0a4d-4654-8ca2-92480b7f3d1b", url);

        requestedURL = "nxpath/default/default-domain/workspaces/foo@view_documents";
        url = LoginScreenHelper.getURLForMobileApplication(BASE_URL, requestedURL);
        assertEquals("nuxeo://http/localhost:8080/nuxeo/default/path/default-domain/workspaces/foo", url);
        url = LoginScreenHelper.getURLForMobileApplication(OTHER_BASE_URL, requestedURL);
        assertEquals("nuxeo://https/demo.nuxeo.com/nuxeo/default/path/default-domain/workspaces/foo", url);

        requestedURL = "nxdoc/default/";
        url = LoginScreenHelper.getURLForMobileApplication(BASE_URL, requestedURL);
        assertEquals("nuxeo://http/localhost:8080/nuxeo/", url);

        requestedURL = "nxhome/not/exist";
        url = LoginScreenHelper.getURLForMobileApplication(OTHER_BASE_URL, requestedURL);
        assertEquals("nuxeo://https/demo.nuxeo.com/nuxeo/", url);
    }
}
