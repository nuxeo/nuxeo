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
package org.nuxeo.ecm.platform.ui.web.restAPI;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestBrowseRestlet extends AbstractRestletTest {

    protected static final String ENDPOINT = "/browse";

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    protected String repositoryName;

    protected DocumentModel root;

    protected DocumentModel doc;

    @Before
    public void before() {
        repositoryName = session.getRepositoryName();
        root = session.getRootDocument();
        doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "sometitle");
        doc = session.createDocument(doc);
        session.save();
        txFeature.nextTransaction();
    }

    @Test
    public void testPatternNothing() throws Exception {
        String path = ENDPOINT;
        doTestPatternNothing(path);
    }

    @Test
    public void testPatternNothingStar() throws Exception {
        String path = "/*" + ENDPOINT;
        doTestPatternNothing(path);
    }

    protected void doTestPatternNothing(String path) throws Exception {
        String expected = XML //
                + "<avalaibleServers>" //
                + "<server title=\"" + repositoryName //
                + "\" url=\"/" + repositoryName + "/*\"/>" //
                + "</avalaibleServers>";
        executeRequest(path, expected);
    }

    @Test
    public void testPatternRepo() throws Exception {
        String path = "/" + repositoryName + ENDPOINT;
        doTestPatternRepo(path);
    }

    @Test
    public void testPatternRepoStar() throws Exception {
        String path = "/" + repositoryName + "/*" + ENDPOINT;
        doTestPatternRepo(path);
    }

    protected void doTestPatternRepo(String path) throws Exception {
        String expectedForDoc = "<document title=\"" + doc.getTitle() //
                + "\" type=\"" + doc.getType() //
                + "\" id=\"" + doc.getId() //
                + "\" name=\"" + doc.getName() //
                + "\" url=\"/" + repositoryName + "/" + doc.getId() + "\"/>";
        String expected = XML //
                + "<document title=\"" + root.getTitle() //
                + "\" type=\"" + root.getType() //
                + "\" id=\"" + root.getId() //
                + "\" url=\"/" + repositoryName + "/" + "\">" //
                + expectedForDoc //
                + "</document>";
        executeRequest(path, expected);
    }

    @Test
    public void testPatternRepoDocId() throws Exception {
        String path = "/" + repositoryName + "/" + doc.getId() + ENDPOINT;

        String expected = XML //
                + "<document title=\"" + doc.getTitle() //
                + "\" type=\"" + doc.getType() //
                + "\" id=\"" + doc.getId() //
                + "\" name=\"" + doc.getName() //
                + "\" url=\"/" + repositoryName + "/" + doc.getId() + "\"/>";
        executeRequest(path, expected);
    }

    @Test
    public void testPatternBadRepo() throws Exception {
        String path = "/nosuchrepo" + ENDPOINT;
        String expected = XML //
                + "<error message=\"Unable to init repository\"/>";
        executeRequest(path, expected);
    }

    @Test
    public void testPatternBadDocId() throws Exception {
        String path = "/" + repositoryName + "/nosuchid" + ENDPOINT;
        String expected = XML //
                + "<error message=\"nosuchid\" class=\"org.nuxeo.ecm.core.api.DocumentNotFoundException\"/>";
        executeRequest(path, expected);
    }

}
