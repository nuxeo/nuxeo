package org.nuxeo.webengine.sites.listeners;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.webengine.sites.listeners.SitesWikiListener.WikiProcessingResult;

import com.google.inject.internal.Lists;

public class TestWikiProcessing extends NXRuntimeTestCase {

    protected Log log = LogFactory.getLog(TestWikiProcessing.class);

    public void testProcessing() throws Exception {
        InputStream is = new FileInputStream(FileUtils
                .getResourceFileFromContext("test-data/page1.wiki"));
        String wikiInput = FileUtils.read(is);
        SitesWikiListener listener = new SitesWikiListener();

        WikiProcessingResult result = listener.processWikiContent(wikiInput,
                "/nuxeo/site/sites", "/nuxeo/site/sites/mypage");
        List<String> relations = result.getRelationLinks();

        List<String> expected = Lists.newArrayList("/nuxeo/site/sites/mypage/WikiPage1",
        		"/nuxeo/site/sites/mypage/WikiPage2", "/nuxeo/site/sites/mypage/Page"); 
        assertTrue(relations.containsAll(expected));

        String content = result.getWikiContent().trim();
        WikiProcessingResult result2 = listener.processWikiContent(content,
                "/nuxeo/site/sites", "/nuxeo/site/sites/mypage");
        String content2 = result2.getWikiContent().trim();
        assertEquals(content, content2);
    }

}
