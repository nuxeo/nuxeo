/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.opensocial.container.server.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.opensocial.container.OpenSocialContainerFeature;
import org.nuxeo.opensocial.container.server.webcontent.OpenSocialAdapterRepositoryInit;
import org.nuxeo.opensocial.container.shared.webcontent.OpenSocialData;
import org.nuxeo.opensocial.container.shared.webcontent.UserPref;
import org.nuxeo.opensocial.container.shared.webcontent.enume.DataType;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(OpenSocialContainerFeature.class)
@RepositoryConfig(init=OpenSocialAdapterRepositoryInit.class, cleanup = Granularity.METHOD)
public class UrlBuilderTest {

    private static final String GADGET_DEF = "http://127.0.0.1:8080/nuxeo/site/gadgets/usw/usw.xml";
    private static final String GADGET_DEF_ENCODED = encode(GADGET_DEF);

    private static final String SERVER_BASE = "http://myserver:8080/nuxeo/";
    private static final String SERVER_BASE_ENCODED = encode("http://myserver:8080/nuxeo/");

    private static final String expectedShindigUrl = SERVER_BASE + "opensocial/gadgets/ifr?"
            + "container=default&nocache=1&country=ALL&lang=ALL&view=default&"
            + "permission=%5B%5D&mid=0&"
            + "parent=" + SERVER_BASE_ENCODED + "&"
            + "url=" + GADGET_DEF_ENCODED + "&"
            + "up_pref1=val%7E1&"
            + "debug=0&"
            + "" // secure token builder disabled
            + "rpctoken=open-social-id1";

    @Test
    public void iCanBuildShindigUrl() throws Exception {
        OpenSocialData data = new OpenSocialData();
        assertNotNull(data);
        data.setGadgetDef(GADGET_DEF);
        data.setId("id1");
        data.setViewer("viewer1");
        data.setOwner("owner1");
        List<UserPref> userPrefs = new ArrayList<UserPref>();
        UserPref pref = new UserPref("pref1", DataType.STRING);
        pref.setActualValue("val~1");
        userPrefs.add(pref);
        data.setUserPrefs(userPrefs);
        UrlBuilder.containerId = 0;
        String actualShindigUrl = UrlBuilder.buildShindigUrl(data, SERVER_BASE);
        assertEquals(expectedShindigUrl, actualShindigUrl);
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

}
