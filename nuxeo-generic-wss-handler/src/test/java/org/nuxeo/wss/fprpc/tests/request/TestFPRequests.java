package org.nuxeo.wss.fprpc.tests.request;

import java.io.InputStream;

import javax.servlet.Filter;

import junit.framework.TestCase;

import org.nuxeo.wss.fprpc.tests.fake.FakeRequest;
import org.nuxeo.wss.fprpc.tests.fake.FakeRequestBuilder;
import org.nuxeo.wss.fprpc.tests.fake.FakeResponse;
import org.nuxeo.wss.servlet.WSSFilter;
import org.nuxeo.wss.spi.WSSListItem;
import org.nuxeo.wss.spi.dummy.DummyMemoryTree;

public class TestFPRequests extends TestCase {

    protected Filter filter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        filter = new WSSFilter();
        filter.init(null);
    }



    public void testPut() throws Exception {
        FakeRequest request = FakeRequestBuilder.buildFromResource("VermeerEncodedPost.dump");
        FakeResponse response = new FakeResponse();

        filter.doFilter(request, response, null);

        WSSListItem item = DummyMemoryTree.instance().getItem("/DocLib0/Workspace-1-1/Document-2-1.doc");
        assertNotNull(item);

        InputStream is = item.getStream();
        assertNotNull(is);

        byte[] buffer = new byte[255];
        StringBuffer sb = new StringBuffer();
        int i ;
        while ((i = is.read(buffer))>0) {
            sb.append(new String(buffer,0,i));
        }

        assertEquals("AAABBBCCCDDD", sb.toString().replace("\n", ""));

        String result= response.getOutput();

        System.out.println(result);

        String[] lines = result.split("\n");
        assertEquals("<p>message=successfully put document 'DocLib0/Workspace-1-1/Document-2-1.doc' as 'DocLib0/Workspace-1-1/Document-2-1.doc'", lines[3]);
    }


}
