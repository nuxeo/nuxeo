package org.nuxeo.webengine.sites.listeners;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.webengine.sites.listeners.SitesWikiListener;
import org.nuxeo.webengine.sites.listeners.SitesWikiListener.WikiProcessingResult;

public class TestWikiProcessing extends NXRuntimeTestCase {

    protected Log log = LogFactory.getLog(TestWikiProcessing.class);

    public void testProcessing() throws Exception {

        InputStream is = TestWikiProcessing.class.getResourceAsStream("test-data/page1.wiki");

        String wikiInput = FileUtils.read(is);

        SitesWikiListener listener = new SitesWikiListener();

        WikiProcessingResult result = listener.processWikiContent(wikiInput,
                "/nuxeo/site/sites", "/nuxeo/site/sites/mypage");

        List<String> relations = result.getRelationLinks();

        String content = result.getWikiContent().trim();

        log.debug(content);

        log.debug(relations);

        WikiProcessingResult result2 = listener.processWikiContent(content,
                "/nuxeo/site/sites", "/nuxeo/site/sites/mypage");

        String content2 = result2.getWikiContent().trim();

        log.debug(content2);

        assertEquals(content, content2);

    }

}
