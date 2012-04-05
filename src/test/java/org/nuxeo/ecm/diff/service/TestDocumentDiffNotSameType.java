/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.service;

import org.junit.runner.RunWith;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.diff.DiffTestCase;
import org.nuxeo.ecm.diff.DocumentDiffNotSameTypeRepositoryInit;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.PropertyType;
import org.nuxeo.ecm.diff.model.SchemaDiff;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * Tests the {@link DocumentDiffService} on documents that are not of the same
 * type. The diff should only hold differences between properties of schemas in
 * common.
 * <p>
 * The {@link DocumentDiffNotSameTypeRepositoryInit} class initializes the
 * repository with 2 documents for this purpose.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(repositoryName = "default", init = DocumentDiffNotSameTypeRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.diff:OSGI-INF/document-xml-export-service.xml",
        "org.nuxeo.diff:OSGI-INF/document-diff-service.xml",
        "org.nuxeo.diff.test:OSGI-INF/test-diff-types-contrib.xml" })
public class TestDocumentDiffNotSameType extends DiffTestCase {

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentDiffService docDiffService;

    /**
     * Tests doc diff.
     *
     * @throws ClientException the client exception
     */
    @Test
    public void testDocDiffNotSameType() throws ClientException {

        // Get left and right docs
        DocumentModel leftDoc = session.getDocument(new PathRef(
                DocumentDiffNotSameTypeRepositoryInit.getLeftDocPath()));
        DocumentModel rightDoc = session.getDocument(new PathRef(
                DocumentDiffNotSameTypeRepositoryInit.getRightDocPath()));

        // Do doc diff
        DocumentDiff docDiff = docDiffService.diff(session, leftDoc, rightDoc);
        assertEquals("Wrong schema count.", 3, docDiff.getSchemaCount());

        // ---------------------------
        // Check system elements
        // ---------------------------
        SchemaDiff schemaDiff = checkSchemaDiff(docDiff, "system", 2);

        // type
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("type"),
                PropertyType.UNDEFINED, "SampleType", "OtherSampleType");
        // path
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("path"),
                PropertyType.UNDEFINED, "leftDoc", "rightDoc");

        // ---------------------------
        // Check dublincore schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "dublincore", 2);

        // title => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("title"),
                PropertyType.STRING, "My first sample, of type SampleType.",
                "My second sample, of type OtherSampleType.");
        // description => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("description"),
                PropertyType.STRING, "description", "Description is different.");

        // ---------------------------
        // Check simpletypes schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "simpletypes", 2);

        // string => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("string"),
                PropertyType.STRING, "a string property",
                "a different string property");
        // boolean => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("boolean"),
                PropertyType.BOOLEAN, String.valueOf(Boolean.TRUE),
                String.valueOf(Boolean.FALSE));
    }
}
