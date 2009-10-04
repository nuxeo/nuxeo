package org.nuxeo.wss.fprpc.tests.resources;

import java.io.InputStream;

import javax.servlet.Filter;

import junit.framework.TestCase;

import org.nuxeo.wss.fprpc.tests.fake.FakeRequest;
import org.nuxeo.wss.fprpc.tests.fake.FakeRequestBuilder;
import org.nuxeo.wss.fprpc.tests.fake.FakeResponse;
import org.nuxeo.wss.handlers.resources.ResourcesHandler;
import org.nuxeo.wss.servlet.WSSFilter;

public class TestResources extends TestCase {


    public void testResourceStream() {

        InputStream is = ResourcesHandler.getResourceAsStream("icons/folder.gif");
        assertNotNull(is);

        is = ResourcesHandler.getResourceAsStream("icons/toto.gif");
        assertNull(is);

        is = ResourcesHandler.getResourceAsStream("ResourceHandler.class");
        assertNull(is);

        is = ResourcesHandler.getResourceAsStream("list-documents.ftl");
        assertNull(is);

    }

    public void testGetResource() throws Exception {
        Filter filter=new WSSFilter();
        filter.init(null);

        FakeRequest request = FakeRequestBuilder.buildFromResource("GetResources.dump");
        FakeResponse response = new FakeResponse();

        filter.doFilter(request, response, null);

        String result= response.getOutput();

        assertTrue(result.startsWith("GIF89"));

    }

}
