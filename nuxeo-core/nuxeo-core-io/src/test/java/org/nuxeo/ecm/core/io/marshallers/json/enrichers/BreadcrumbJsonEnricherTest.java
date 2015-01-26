/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import javax.inject.Inject;

@LocalDeploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
public class BreadcrumbJsonEnricherTest extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    public BreadcrumbJsonEnricherTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    private DocumentModel document;

    @Inject
    private CoreSession session;

    @Before
    public void setup() {
        document = session.createDocumentModel("/", "level1", "RefDoc");
        document = session.createDocument(document);
        document = session.createDocumentModel("/level1", "level2", "RefDoc");
        document = session.createDocument(document);
        document = session.createDocumentModel("/level1/level2", "level3", "RefDoc");
        document = session.createDocument(document);
    }

    @Test
    public void test() throws Exception {
        JsonAssert json = jsonAssert(document, CtxBuilder.enrichDoc("breadcrumb").get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has("breadcrumb").isObject();
        json.has("entity-type").isEquals("documents");
        json = json.has("entries").length(3);
        for (int i = 0; i < 3; i++) {
            JsonAssert doc = json.has(i);
            doc.has("entity-type").isEquals("document");
            doc.has("title").isEquals("level" + (i + 1));
        }
    }

}