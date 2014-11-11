/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.webengine.sites.listeners;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.webengine.sites.listeners.SitesWikiListener.WikiProcessingResult;

import com.google.common.collect.Lists;

public class TestWikiProcessing extends NXRuntimeTestCase {

    protected Log log = LogFactory.getLog(TestWikiProcessing.class);

    @Test
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
