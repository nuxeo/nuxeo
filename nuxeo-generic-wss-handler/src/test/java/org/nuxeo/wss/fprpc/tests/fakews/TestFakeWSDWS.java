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
import org.nuxeo.wss.handlers.fakews.FakeDWS;
import org.nuxeo.wss.handlers.fakews.FakeWSCmdParser;
import org.nuxeo.wss.servlet.WSSFilter;
import org.nuxeo.wss.servlet.WSSRequest;

public class TestFakeWSDWS {

    @Test
    public void testParsing() throws Exception {

        FakeRequest request = FakeRequestBuilder.buildFromResource("GetDwsMetaData.dump");

        WSSRequest wsrequest = new WSSRequest(request, null);

        String url = new FakeWSCmdParser(FakeDWS.document_TAG).getParameter(wsrequest);

        assertEquals("http://localhost/DocLib0/Workspace-1-1/Document-2-1.doc", url);
    }

    @Test
    public void testHandling() throws Exception {

        Filter filter=new WSSFilter();
        filter.init(null);

        FakeRequest request = FakeRequestBuilder.buildFromResource("GetDwsMetaData.dump");
        FakeResponse response = new FakeResponse();

        filter.doFilter(request, response, null);

        String result= response.getOutput();

        // System.out.println(result);

        String[] lines = result.split("\n");

        //assertEquals("&lt;DocUrl&gt;http://localhost/DocLib0/Workspace-1-1/Document-2-1.doc&lt;/DocUrl&gt;", lines[27].trim());

    }

}
