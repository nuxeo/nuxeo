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

package org.nuxeo.wss.fprpc.tests.request;

import static org.junit.Assert.assertEquals;

import javax.servlet.Filter;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.wss.fprpc.tests.WindowsHelper;
import org.nuxeo.wss.fprpc.tests.fake.FakeRequest;
import org.nuxeo.wss.fprpc.tests.fake.FakeRequestBuilder;
import org.nuxeo.wss.fprpc.tests.fake.FakeResponse;
import org.nuxeo.wss.servlet.WSSFilter;

public class TestInfoRequests {

    protected Filter filter;

    @Before
    public void setUp() throws Exception {
        filter=new WSSFilter();
        filter.init(null);
    }

    @Test
    public void testVTIInfo() throws Exception {

        FakeRequest request = FakeRequestBuilder.buildFromResource("VTI-INFO.dump");
        FakeResponse response = new FakeResponse();

        filter.doFilter(request, response, null);

        String result= response.getOutput();

        // System.out.println(result);

        String[] lines = WindowsHelper.splitLines(result);
        assertEquals("FPVersion=\"12.0.0.000\"", lines[1]);
        assertEquals("FPShtmlScriptUrl=\"_vti_bin/shtml.dll/_vti_rpc\"", lines[2]);
        assertEquals("FPAuthorScriptUrl=\"_vti_bin/_vti_aut/author.dll\"", lines[3]);
        assertEquals("FPAdminScriptUrl=\"_vti_bin/_vti_adm/admin.dll\"", lines[4]);
        assertEquals("TPScriptUrl=\"_vti_bin/owssvr.dll\"", lines[5]);

    }

    @Test
    public void testServerVersion() throws Exception {

        FakeRequest request = FakeRequestBuilder.buildFromResource("ServerVersion.dump");
        FakeResponse response = new FakeResponse();

        filter.doFilter(request, response, null);

        String result= response.getOutput();

        // System.out.println(result);

        String[] lines = WindowsHelper.splitLines(result);

        assertEquals("<p>method=server version:6.0.2.5523", lines[2]);
        assertEquals("<li>ver incr=6421", lines[8]);

    }

    @Test
    public void testOpenService() throws Exception {

        FakeRequest request = FakeRequestBuilder.buildFromResource("OpenService.dump");
        FakeResponse response = new FakeResponse();

        filter.doFilter(request, response, null);

        String result= response.getOutput();

        // System.out.println(result);

        String[] lines = WindowsHelper.splitLines(result);

        assertEquals("<p>method=open service:6.0.2.5523", lines[2]);
        assertEquals("<li>SX|http://localhost/_layouts/toolpane.aspx", lines[17]);

    }

    @Test
    public void testHead() throws Exception {

        FakeRequest request = FakeRequestBuilder.buildFromResource("HeadOwsSrv.dump");
        FakeResponse response = new FakeResponse();

        filter.doFilter(request, response, null);

        String result= response.getOutput();

        // System.out.println(result);

    }




}
