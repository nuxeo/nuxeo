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

import java.io.InputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.wss.fprpc.FPRPCCall;
import org.nuxeo.wss.fprpc.FPRPCRequest;
import org.nuxeo.wss.fprpc.tests.fake.FakeRequest;
import org.nuxeo.wss.fprpc.tests.fake.FakeRequestBuilder;

public class TestRequestParsing {

    @Test
    public void testGETParsing() throws Exception {

        FakeRequest request = FakeRequestBuilder.buildFromResource("SimpleGETRequest.dump");
        assertEquals("GET",request.getMethod());
        assertEquals(3,request.getParameterMap().size());
        assertEquals("Method_name",request.getParameter("Cmd"));

        FPRPCRequest fpRequest = new FPRPCRequest(request, null);

        assertEquals(FPRPCRequest.FPRPC_GET_REQUEST, fpRequest.getRequestMode());
        assertEquals(1, fpRequest.getCalls().size());
        FPRPCCall call = fpRequest.getCalls().get(0);
        assertNotNull(call);

        assertEquals("Method_name",call.getMethodName());
        assertEquals(2, call.getParameters().size());
        assertEquals("Value1",call.getParameters().get("Parameter1"));
        assertEquals("Value2",call.getParameters().get("Parameter2"));
    }

    @Test
    public void testPOSTParsing() throws Exception {

        FakeRequest request = FakeRequestBuilder.buildFromResource("SimplePOSTRequest.dump");
        assertEquals("POST",request.getMethod());
        assertEquals(15,request.getParameterMap().size());
        assertEquals("list documents",request.getParameter("method"));

        FPRPCRequest fpRequest = new FPRPCRequest(request, null);

        assertEquals(FPRPCRequest.FPRPC_POST_REQUEST, fpRequest.getRequestMode());
        assertEquals(1, fpRequest.getCalls().size());
        FPRPCCall call = fpRequest.getCalls().get(0);
        assertNotNull(call);

        assertEquals("list documents",call.getMethodName());
        assertEquals(14, call.getParameters().size());
        assertEquals("false",call.getParameters().get("listRecurse"));
        assertEquals("true",call.getParameters().get("listIncludeParent"));

    }

    @Test
    public void testCAMLParsing() throws Exception {

        FakeRequest request = FakeRequestBuilder.buildFromResource("SimpleCAMLRequest.dump");
        assertEquals("POST",request.getMethod());
        assertEquals(1,request.getParameterMap().size());
        assertEquals("DisplayPost",request.getParameter("Cmd"));

        FPRPCRequest fpRequest = new FPRPCRequest(request, null);

        assertEquals(FPRPCRequest.FPRPC_CAML_REQUEST, fpRequest.getRequestMode());
        assertEquals(2, fpRequest.getCalls().size());

        FPRPCCall call = fpRequest.getCalls().get(0);
        assertNotNull(call);
        assertEquals("NewList",call.getMethodName());
        assertEquals(2, call.getParameters().size());
        assertEquals("Meeting Topics",call.getParameters().get("Title"));

        call = fpRequest.getCalls().get(1);
        assertNotNull(call);
        assertEquals("NewList",call.getMethodName());
        assertEquals(2, call.getParameters().size());
        assertEquals("Volunteers",call.getParameters().get("Title"));


    }

    @Test
    public void testMatching() {

        String regexp = "^\\/([^/]*)\\/([^/]*)\\/([^?]*)\\S*$";

        // "^\\/([^/]*)\\/([^/]*)\\/([^?]*)\\.dll\\S*$"

        String data = "/toto/titi/something";

        Pattern pattern = Pattern.compile(regexp);

        Matcher matcher = pattern.matcher(data);

        assertTrue(matcher.matches());

    }

    @Test
    public void testVermeerEncodingParsing() throws Exception {

        FakeRequest request = FakeRequestBuilder.buildFromResource("VermeerEncodedPost.dump");
        assertEquals("POST",request.getMethod());

        FPRPCRequest fpRequest = new FPRPCRequest(request, null);

        assertEquals(FPRPCRequest.FPRPC_POST_REQUEST, fpRequest.getRequestMode());
        assertEquals(1, fpRequest.getCalls().size());

        FPRPCCall call = fpRequest.getCalls().get(0);
        assertNotNull(call);
        assertEquals("put document",call.getMethodName());

        Map<String, String> params = call.getParameters();
        assertNotNull(params);
        assertTrue(params.containsKey("service_name"));
        assertTrue(params.containsKey("document/document_name"));
        assertTrue(params.containsKey("put_option"));

        assertEquals("DocLib0/Workspace-1-1/Document-2-1.doc", params.get("document/document_name"));

        InputStream is = fpRequest.getVermeerBinary();
        assertNotNull(is);

        byte[] buffer = new byte[255];
        StringBuffer sb = new StringBuffer();
        int i ;
        while ((i = is.read(buffer))>0) {
            sb.append(new String(buffer,0,i));
        }

        assertEquals("AAABBBCCCDDD", sb.toString().replace("\n", ""));
    }


}
