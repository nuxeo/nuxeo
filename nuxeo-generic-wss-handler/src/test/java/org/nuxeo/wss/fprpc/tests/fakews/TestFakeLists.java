package org.nuxeo.wss.fprpc.tests.fakews;

import javax.servlet.Filter;

import org.nuxeo.wss.fprpc.tests.fake.FakeRequest;
import org.nuxeo.wss.fprpc.tests.fake.FakeRequestBuilder;
import org.nuxeo.wss.fprpc.tests.fake.FakeResponse;
import org.nuxeo.wss.servlet.WSSFilter;

import junit.framework.TestCase;

public class TestFakeLists extends TestCase {

	
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
