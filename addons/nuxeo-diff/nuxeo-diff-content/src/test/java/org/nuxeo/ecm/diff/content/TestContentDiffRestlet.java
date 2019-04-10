/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.diff.content;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;

import java.io.File;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.ui.web.restAPI.AbstractRestletTest;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD, init = ContentDiffRepositoryInit.class)
@Deploy("org.nuxeo.ecm.platform.convert:OSGI-INF/convert-service-contrib.xml")
@Deploy("org.nuxeo.diff.content")
public class TestContentDiffRestlet extends AbstractRestletTest {

    protected static final String ENDPOINT = "/contentDiff";

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    protected String repositoryName;

    protected DocumentModel left;

    protected DocumentModel right;

    @Before
    public void before() {
        repositoryName = session.getRepositoryName();
        left = session.getDocument(new PathRef(ContentDiffRepositoryInit.getLeftPlainTextDocPath()));
        right = session.getDocument(new PathRef(ContentDiffRepositoryInit.getRightPlainTextDocPath()));
    }

    @Test
    public void testContentDiff() throws Exception {
        String path = ENDPOINT + "/" + repositoryName + "/" + left.getId() + "/" + right.getId() + "/default/";
        String content = executeRequest(path, HttpGet::new, SC_OK, "text/html;charset=UTF-8",
                "inline; filename*=UTF-8''contentDiff.html");
        checkContentDiff("plain_text_content_diff.html", content);
    }

    protected void checkContentDiff(String expectedPath, String actual) throws Exception {
        File file = org.nuxeo.common.utils.FileUtils.getResourceFileFromContext(expectedPath);
        String expected = FileUtils.readFileToString(file, UTF_8);
        if (SystemUtils.IS_OS_WINDOWS) {
            // make tests pass under Windows
            expected = expected.trim();
            expected = expected.replace("\n", "");
            expected = expected.replace("\r", "");
            actual = actual.trim();
            actual = actual.replace("\n", "");
            actual = actual.replace("\r", "");
        }
        assertEquals(expected, actual);
    }

}
