package org.nuxeo.ecm.platform.webengine.jsf.wiki;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;

public class WikiPageDocResourceLocator implements ResourceLocator {

    protected DocumentModel wikiPage;
    protected File wikiFile;


    public void cleanup()
    {
        if (wikiFile!=null && wikiFile.exists())
            wikiFile.delete();
    }

    public WikiPageDocResourceLocator(DocumentModel wikiPage)
    {
        this.wikiPage=wikiPage;
    }

    private File getTempFileFromWikiPage() throws IOException
    {
        String wikiContent = (String) wikiPage.getProperty("wikiPage", "content");

        wikiFile = File.createTempFile("wikiContent", wikiPage.getId());

        FileWriter fw = new FileWriter(wikiFile);

        fw.write(wikiContent);
        fw.close();

        return wikiFile;
    }

    public File getResourceFile(String key) {
        try {
            File file = getTempFileFromWikiPage();
            return file;
        } catch (IOException e) {
            return null;
        }
    }

    public URL getResourceURL(String key) {
        try {
            File file = getTempFileFromWikiPage();
            URL url = new URL("file://" + file.getAbsolutePath());
            return url;
        } catch (IOException e) {
            return null;
        }
    }

}
