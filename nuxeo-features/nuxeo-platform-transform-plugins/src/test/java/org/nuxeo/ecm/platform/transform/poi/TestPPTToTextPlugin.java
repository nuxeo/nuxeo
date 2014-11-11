package org.nuxeo.ecm.platform.transform.poi;

import java.io.File;
import java.util.List;

import org.nuxeo.ecm.platform.transform.AbstractPluginTestCase;
import org.nuxeo.ecm.platform.transform.DocumentTestUtils;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.poi.PowerpointToTextPlugin;
import org.nuxeo.ecm.platform.transform.timer.SimpleTimer;

public class TestPPTToTextPlugin extends AbstractPluginTestCase {

    private PowerpointToTextPlugin plugin;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        plugin = (PowerpointToTextPlugin) service.getPluginByName("ppt2text_poi");
    }

    @Override
    public void tearDown() throws Exception {
        plugin = null;
        super.tearDown();
    }

    public void testSmallPpt2textConversion() throws Exception {
        String path = "test-data/hello.ppt";

        SimpleTimer timer = new SimpleTimer();
        timer.start();
        List<TransformDocument> results = plugin.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path)));
        timer.stop();
        System.out.println(timer);

        File textFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "txt");
        assertEquals("text content", "Hello from a Microsoft PowerPoint Presentation!",
                DocumentTestUtils.readContent(textFile));
    }

}
