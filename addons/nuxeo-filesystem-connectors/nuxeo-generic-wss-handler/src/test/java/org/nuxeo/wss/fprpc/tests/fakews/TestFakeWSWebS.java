/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.wss.fprpc.tests.fakews;

import javax.servlet.Filter;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.wss.fprpc.tests.fake.FakeRequest;
import org.nuxeo.wss.fprpc.tests.fake.FakeRequestBuilder;
import org.nuxeo.wss.fprpc.tests.fake.FakeResponse;
import org.nuxeo.wss.handlers.fakews.FakeWebS;
import org.nuxeo.wss.handlers.fakews.FakeWSCmdParser;
import org.nuxeo.wss.servlet.WSSFilter;
import org.nuxeo.wss.servlet.WSSRequest;

public class TestFakeWSWebS {

    @Test
    public void testParsing() throws Exception {

        FakeRequest request = FakeRequestBuilder.buildFromResource("WebUrlFromPageUrl.dump");

        WSSRequest wsrequest = new WSSRequest(request, null);

        String url = new FakeWSCmdParser(FakeWebS.pageUrl_TAG).getParameter(wsrequest);

        assertEquals("http://vm2k3/MyDocList/Nuxeo%20Annotation%20Service.doc", url);
    }

    @Test
    public void testHandling() throws Exception {

        Filter filter=new WSSFilter();
        filter.init(null);

        FakeRequest request = FakeRequestBuilder.buildFromResource("WebUrlFromPageUrl.dump");
        FakeResponse response = new FakeResponse();

        filter.doFilter(request, response, null);

        String result= response.getOutput();

        //System.out.println(result);

        String[] lines = result.split("\n");

        assertEquals("<WebUrlFromPageUrlResult>http://localhost/</WebUrlFromPageUrlResult>", lines[4].trim());

    }

    @Test
    public void testHandlingGetWebCollection() throws Exception {

        Filter filter=new WSSFilter();
        filter.init(null);

        FakeRequest request = FakeRequestBuilder.buildFromResource("GetWebCollection.dump");
        FakeResponse response = new FakeResponse();

        filter.doFilter(request, response, null);

        String result= response.getOutput();

        System.out.println(result);

        //String[] lines = result.split("\n");

        //assertEquals("<WebUrlFromPageUrlResult>http://localhost/</WebUrlFromPageUrlResult>", lines[4].trim());

    }


}
