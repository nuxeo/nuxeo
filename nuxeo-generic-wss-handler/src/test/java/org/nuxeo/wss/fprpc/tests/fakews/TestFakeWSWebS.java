package org.nuxeo.wss.fprpc.tests.fakews;

import javax.servlet.Filter;

import junit.framework.TestCase;

import org.nuxeo.wss.fprpc.tests.fake.FakeRequest;
import org.nuxeo.wss.fprpc.tests.fake.FakeRequestBuilder;
import org.nuxeo.wss.fprpc.tests.fake.FakeResponse;
import org.nuxeo.wss.handlers.fakews.FakeWebS;
import org.nuxeo.wss.handlers.fakews.FakeWSCmdParser;
import org.nuxeo.wss.servlet.WSSFilter;
import org.nuxeo.wss.servlet.WSSRequest;

public class TestFakeWSWebS extends TestCase {

    public void testParsing() throws Exception {

        FakeRequest request = FakeRequestBuilder.buildFromResource("WebUrlFromPageUrl.dump");

        WSSRequest wsrequest = new WSSRequest(request, null);

        String url = new FakeWSCmdParser(FakeWebS.pageUrl_TAG).getParameter(wsrequest);

        assertEquals("http://vm2k3/MyDocList/Nuxeo%20Annotation%20Service.doc", url);
    }


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



}
