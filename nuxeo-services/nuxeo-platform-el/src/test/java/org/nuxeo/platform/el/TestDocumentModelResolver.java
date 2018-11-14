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
 *     Funsho David
 */
package org.nuxeo.platform.el;

import static org.junit.Assert.assertEquals;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.inject.Inject;

import org.jboss.el.ExpressionFactoryImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 8.10-HF39
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
public class TestDocumentModelResolver {

    private final ExpressionTester et = new ExpressionTester(new ExpressionFactoryImpl());

    private class ExpressionTester extends ExpressionEvaluator {
        public ExpressionTester(ExpressionFactory factory) {
            super(factory);
        }

        public void setValue(ELContext context, String stringExpression, Object value) {
            ValueExpression ve = expressionFactory.createValueExpression(context, stringExpression, Object.class);
            ve.setValue(context, value);
        }
    }

    private final ExpressionContext context = new ExpressionContext();

    @Inject
    protected CoreSession session;

    @Test
    @SuppressWarnings("unchecked")
    public void testFileWithBlobsResolver() {

        DocumentModel doc = session.createDocumentModel("/", "testFile", "File");
        doc = session.createDocument(doc);
        et.bindValue(context, "doc", doc);
        doc.addFacet("WithFiles");

        List<Map<String, Serializable>> fileList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            StringBlob blob = new StringBlob("sample content " + i);
            blob.setFilename("testcontent" + i + ".txt");
            fileList.add(Collections.singletonMap("file", blob));
        }
        doc.setPropertyValue("files:files", (Serializable) fileList);

        et.setValue(context, "${doc.files.files[0].file}", null);

        fileList = (List<Map<String, Serializable>>) doc.getPropertyValue("files:files");
        assertEquals(9, fileList.size());
    }
}
