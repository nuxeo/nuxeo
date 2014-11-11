import java.io.InputStream;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.webengine.sites.listeners.SitesWikiListener;
import org.nuxeo.webengine.sites.listeners.SitesWikiListener.WikiProcessingResult;


public class TestWikiProcessing extends NXRuntimeTestCase {

    public void testProcessing() throws Exception {

        InputStream is = TestWikiProcessing.class.getResourceAsStream("test-data/page1.wiki");

        String wikiInput = FileUtils.read(is);

        SitesWikiListener listener = new SitesWikiListener();

        WikiProcessingResult  result = listener.processWikiContent(wikiInput, "/nuxeo/site/sites", "/nuxeo/site/sites/mypage");

        List<String> relations = result.getRelationLinks();

        String content = result.getWikiContent().trim();

        System.out.println(content);

        System.out.println(relations);

        WikiProcessingResult  result2 = listener.processWikiContent(content, "/nuxeo/site/sites", "/nuxeo/site/sites/mypage");

        String content2 = result2.getWikiContent().trim();

        System.out.println(content2);

        assertEquals(content, content2);

    }

}
