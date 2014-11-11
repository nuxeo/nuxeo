/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.webengine.jsf.wiki;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;

public class WikiPageDocResourceLocator implements ResourceLocator {

    protected final DocumentModel wikiPage;
    protected File wikiFile;

    public void cleanup() {
        if (wikiFile != null && wikiFile.exists()) {
            wikiFile.delete();
        }
    }

    public WikiPageDocResourceLocator(DocumentModel wikiPage) {
        this.wikiPage = wikiPage;
    }

    private File getTempFileFromWikiPage() throws IOException {
        String wikiContent = (String) wikiPage.getProperty("wikiPage", "content");

        wikiFile = File.createTempFile("wikiContent", wikiPage.getId());

        FileWriter fw = new FileWriter(wikiFile);

        fw.write(wikiContent);
        fw.close();

        return wikiFile;
    }

    public File getResourceFile(String key) {
        try {
            return getTempFileFromWikiPage();
        } catch (IOException e) {
            return null;
        }
    }

    public URL getResourceURL(String key) {
        try {
            File file = getTempFileFromWikiPage();
            return new URL("file://" + file.getAbsolutePath());
        } catch (IOException e) {
            return null;
        }
    }

}
