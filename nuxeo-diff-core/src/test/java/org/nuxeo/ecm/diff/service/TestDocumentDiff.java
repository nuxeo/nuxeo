/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.DocumentXMLExporter;
import org.nuxeo.ecm.core.storage.binary.Binary;
import org.nuxeo.ecm.core.storage.binary.BinaryManager;
import org.nuxeo.ecm.core.storage.binary.BinaryManagerDescriptor;
import org.nuxeo.ecm.core.storage.binary.DefaultBinaryManager;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.diff.DiffTestCase;
import org.nuxeo.ecm.diff.model.DifferenceType;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.PropertyType;
import org.nuxeo.ecm.diff.model.SchemaDiff;
import org.nuxeo.ecm.diff.model.impl.ComplexPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.ContentProperty;
import org.nuxeo.ecm.diff.model.impl.ContentPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.ListPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.SimplePropertyDiff;
import org.nuxeo.ecm.diff.test.DocumentDiffRepositoryInit;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * Tests the {@link DocumentDiffService} on documents of the same type.
 * <p>
 * The {@link DocumentDiffRepositoryInit} class initializes the repository with 2 documents for this purpose.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = DocumentDiffRepositoryInit.class)
@Deploy({ "org.nuxeo.ecm.core.io:OSGI-INF/document-xml-exporter-service.xml", "org.nuxeo.diff.core",
        "org.nuxeo.diff.test" })
public class TestDocumentDiff extends DiffTestCase {

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentDiffService docDiffService;

    @Inject
    protected DocumentXMLExporter docXMLExporter;

    protected BinaryManager binaryManager;

    @Before
    public void setUp() throws ClientException {
        binaryManager = new DefaultBinaryManager();
        try {
            binaryManager.initialize(new BinaryManagerDescriptor());
        } catch (IOException ioe) {
            throw new ClientException("Error while initializing binary manager", ioe);
        }
    }

    @After
    public void tearDown() {
        binaryManager.close();
    }

    /**
     * Tests doc diff.
     *
     * @throws ClientException the client exception
     */
    @Test
    public void testDocDiff() throws ClientException {

        // Get left and right docs
        DocumentModel leftDoc = session.getDocument(new PathRef(DocumentDiffRepositoryInit.getLeftDocPath()));
        DocumentModel rightDoc = session.getDocument(new PathRef(DocumentDiffRepositoryInit.getRightDocPath()));

        // Do doc diff
        DocumentDiff docDiff = docDiffService.diff(session, leftDoc, rightDoc);
        assertEquals("Wrong schema count.", 6, docDiff.getSchemaCount());

        // ---------------------------
        // Check dublincore schema
        // ---------------------------
        SchemaDiff schemaDiff = checkSchemaDiff(docDiff, "dublincore", 6);

        // title => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("title"), PropertyType.STRING, "My first sample",
                "My second sample");
        // description => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("description"), PropertyType.STRING, "description", null);
        // created => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("created"), PropertyType.DATE, "2011-12-29T11:24:25.00Z",
                "2011-12-29T11:24:50.00Z");
        // creator => same
        checkIdenticalField(schemaDiff.getFieldDiff("creator"));
        // modified => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("modified"), PropertyType.DATE, "2011-12-29T11:24:25.00Z",
                "2011-12-30T12:05:02.00Z");
        // lastContributor => same once trimmed
        checkIdenticalField(schemaDiff.getFieldDiff("lastContributor"));
        // contributors => different (update) / same / different (add)
        ListPropertyDiff expectedListFieldDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedListFieldDiff.putDiff(0, new SimplePropertyDiff(PropertyType.STRING, "Administrator",
                "anotherAdministrator"));
        expectedListFieldDiff.putDiff(2, new SimplePropertyDiff(PropertyType.STRING, null, "jack"));
        checkListFieldDiff(schemaDiff.getFieldDiff("contributors"), expectedListFieldDiff);
        // subjects => same / different (remove)
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedListFieldDiff.putDiff(1, new SimplePropertyDiff(PropertyType.STRING, "Architecture", null));
        checkListFieldDiff(schemaDiff.getFieldDiff("subjects"), expectedListFieldDiff);

        // ---------------------------
        // Check file schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "file", 2);

        // filename => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("filename"), PropertyType.STRING, "Joe.txt", "Jack.txt");
        // content => different
        ContentPropertyDiff expectedContentFieldDiff = new ContentPropertyDiff(DifferenceType.different);
        Blob leftBlob = (Blob) leftDoc.getPropertyValue("file:content");
        Blob rightBlob = (Blob) rightDoc.getPropertyValue("file:content");
        expectedContentFieldDiff.setLeftContent(new ContentProperty(null, null, "Joe.txt", getDigest(leftBlob)));
        expectedContentFieldDiff.setRightContent(new ContentProperty(null, null, "Jack.txt", getDigest(rightBlob)));
        checkContentFieldDiff(schemaDiff.getFieldDiff("content"), expectedContentFieldDiff);

        // ---------------------------
        // Check files schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "files", 1);

        // files =>
        // item1: same
        // item2: different filename
        // item3: different content
        // item4: remove
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);

        ComplexPropertyDiff item1ExpectedComplexFieldDiff = new ComplexPropertyDiff();
        item1ExpectedComplexFieldDiff.putDiff("filename", new SimplePropertyDiff(PropertyType.STRING,
                "second_attachement.txt", "the_file_name_is_different.txt"));
        item1ExpectedComplexFieldDiff.putDiff("file", new ContentPropertyDiff(DifferenceType.differentFilename,
                new ContentProperty(null, null, "second_attachement.txt", null), new ContentProperty(null, null,
                        "the_file_name_is_different.txt", null)));

        ComplexPropertyDiff item2ExpectedComplexFieldDiff = new ComplexPropertyDiff();
        leftBlob = (Blob) leftDoc.getPropertyValue("files:files/2/file");
        rightBlob = (Blob) rightDoc.getPropertyValue("files:files/2/file");
        item2ExpectedComplexFieldDiff.putDiff("file", new ContentPropertyDiff(DifferenceType.differentDigest,
                new ContentProperty(null, null, null, getDigest(leftBlob)), new ContentProperty(null, null, null,
                        getDigest(rightBlob))));

        ComplexPropertyDiff item3ExpectedComplexFieldDiff = new ComplexPropertyDiff();
        item3ExpectedComplexFieldDiff.putDiff("filename", new SimplePropertyDiff(PropertyType.STRING,
                "fourth_attachement.txt", null));
        leftBlob = (Blob) leftDoc.getPropertyValue("files:files/3/file");
        item3ExpectedComplexFieldDiff.putDiff("file", new ContentPropertyDiff(DifferenceType.different,
                new ContentProperty("UTF-8", "text/plain", "fourth_attachement.txt", getDigest(leftBlob)),
                new ContentProperty()));

        expectedListFieldDiff.putDiff(1, item1ExpectedComplexFieldDiff);
        expectedListFieldDiff.putDiff(2, item2ExpectedComplexFieldDiff);
        expectedListFieldDiff.putDiff(3, item3ExpectedComplexFieldDiff);

        checkListFieldDiff(schemaDiff.getFieldDiff("files"), expectedListFieldDiff);

        // ---------------------------
        // Check simpletypes schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "simpletypes", 4);

        // string => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("string"), PropertyType.STRING, "a string property",
                "a different string property");
        // textarea => same
        checkIdenticalField(schemaDiff.getFieldDiff("textarea"));
        // boolean => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("boolean"), PropertyType.BOOLEAN, String.valueOf(Boolean.TRUE),
                null);
        // integer => same
        checkIdenticalField(schemaDiff.getFieldDiff("integer"));
        // date => same
        checkIdenticalField(schemaDiff.getFieldDiff("date"));
        // htmlText => different
        checkSimpleFieldDiff(
                schemaDiff.getFieldDiff("htmlText"),
                PropertyType.STRING,
                "&lt;p&gt;html text with &lt;strong&gt;&lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/strong&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;",
                "&lt;p&gt;html  text modified with &lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;\n&lt;p&gt;&amp;nbsp;&lt;/p&gt;");
        // multivalued => different (remove) * 4
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedListFieldDiff.putDiff(0, new SimplePropertyDiff(PropertyType.STRING, "monday", null));
        expectedListFieldDiff.putDiff(1, new SimplePropertyDiff(PropertyType.STRING, "tuesday", null));
        expectedListFieldDiff.putDiff(2, new SimplePropertyDiff(PropertyType.STRING, "wednesday", null));
        expectedListFieldDiff.putDiff(3, new SimplePropertyDiff(PropertyType.STRING, "thursday", null));
        checkListFieldDiff(schemaDiff.getFieldDiff("multivalued"), expectedListFieldDiff);

        // ---------------------------
        // Check complextypes schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "complextypes", 2);

        // complex => same / different (update) / different (remove) / different
        // (add)
        ComplexPropertyDiff expectedComplexFieldDiff = new ComplexPropertyDiff();
        expectedComplexFieldDiff.putDiff(
                "booleanItem",
                new SimplePropertyDiff(PropertyType.BOOLEAN, String.valueOf(Boolean.TRUE),
                        String.valueOf(Boolean.FALSE)));
        expectedComplexFieldDiff.putDiff("integerItem", new SimplePropertyDiff(PropertyType.LONG, "10", null));
        expectedComplexFieldDiff.putDiff("dateItem", new SimplePropertyDiff(PropertyType.DATE, null,
                "2011-12-29T23:00:00.00Z"));
        checkComplexFieldDiff(schemaDiff.getFieldDiff("complex"), expectedComplexFieldDiff);

        // complexList =>
        // item1: same / different (update) / different (remove) / different
        // (add)
        // item2: add
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);

        item1ExpectedComplexFieldDiff = new ComplexPropertyDiff();
        item1ExpectedComplexFieldDiff.putDiff(
                "booleanItem",
                new SimplePropertyDiff(PropertyType.BOOLEAN, String.valueOf(Boolean.TRUE),
                        String.valueOf(Boolean.FALSE)));
        item1ExpectedComplexFieldDiff.putDiff("integerItem", new SimplePropertyDiff(PropertyType.LONG, "12", null));
        item1ExpectedComplexFieldDiff.putDiff("dateItem", new SimplePropertyDiff(PropertyType.DATE, null,
                "2011-12-30T23:00:00.00Z"));

        item2ExpectedComplexFieldDiff = new ComplexPropertyDiff();
        item2ExpectedComplexFieldDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, null,
                "second element of a complex list"));
        item2ExpectedComplexFieldDiff.putDiff("booleanItem",
                new SimplePropertyDiff(PropertyType.BOOLEAN, null, String.valueOf(Boolean.FALSE)));
        item2ExpectedComplexFieldDiff.putDiff("integerItem", new SimplePropertyDiff(PropertyType.LONG, null, "20"));
        item2ExpectedComplexFieldDiff.putDiff("dateItem", new SimplePropertyDiff(PropertyType.DATE, null, ""));

        expectedListFieldDiff.putDiff(0, item1ExpectedComplexFieldDiff);
        expectedListFieldDiff.putDiff(1, item2ExpectedComplexFieldDiff);

        checkListFieldDiff(schemaDiff.getFieldDiff("complexList"), expectedListFieldDiff);

        // ---------------------------
        // Check listoflists schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "listoflists", 1);

        // listOfLists =>
        // item1: same
        // item2: different
        // item3: add
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);

        item1ExpectedComplexFieldDiff = new ComplexPropertyDiff();
        item1ExpectedComplexFieldDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, "second item",
                "second item is different"));
        ListPropertyDiff expectedNestedListDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedNestedListDiff.putDiff(1, new SimplePropertyDiff(PropertyType.STRING, "Thursday", "Friday"));
        expectedNestedListDiff.putDiff(2, new SimplePropertyDiff(PropertyType.STRING, null, "Saturday"));
        expectedNestedListDiff.putDiff(3, new SimplePropertyDiff(PropertyType.STRING, null, "July"));
        expectedNestedListDiff.putDiff(4, new SimplePropertyDiff(PropertyType.STRING, null, "August"));
        item1ExpectedComplexFieldDiff.putDiff("stringListItem", expectedNestedListDiff);

        item2ExpectedComplexFieldDiff = new ComplexPropertyDiff();
        item2ExpectedComplexFieldDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, null,
                "third item"));
        item2ExpectedComplexFieldDiff.putDiff("stringListItem", new ListPropertyDiff(PropertyType.SCALAR_LIST));

        expectedListFieldDiff.putDiff(1, item1ExpectedComplexFieldDiff);
        expectedListFieldDiff.putDiff(2, item2ExpectedComplexFieldDiff);

        checkListFieldDiff(schemaDiff.getFieldDiff("listOfLists"), expectedListFieldDiff);

    }

    /**
     * Tests inverse doc diff.
     *
     * @throws ClientException the client exception
     */
    @Test
    public void testInverseDocDiff() throws ClientException {

        // Get left and right docs
        DocumentModel leftDoc = session.getDocument(new PathRef(DocumentDiffRepositoryInit.getRightDocPath()));
        DocumentModel rightDoc = session.getDocument(new PathRef(DocumentDiffRepositoryInit.getLeftDocPath()));

        // Do doc diff
        DocumentDiff docDiff = docDiffService.diff(session, leftDoc, rightDoc);
        assertEquals("Wrong schema count.", 6, docDiff.getSchemaCount());

        // ---------------------------
        // Check dublincore schema
        // ---------------------------
        SchemaDiff schemaDiff = checkSchemaDiff(docDiff, "dublincore", 6);

        // title => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("title"), PropertyType.STRING, "My second sample",
                "My first sample");
        // description => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("description"), PropertyType.STRING, null, "description");
        // created => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("created"), PropertyType.DATE, "2011-12-29T11:24:50.00Z",
                "2011-12-29T11:24:25.00Z");
        // creator => same
        checkIdenticalField(schemaDiff.getFieldDiff("creator"));
        // modified => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("modified"), PropertyType.DATE, "2011-12-30T12:05:02.00Z",
                "2011-12-29T11:24:25.00Z");
        // lastContributor => same once trimmed
        checkIdenticalField(schemaDiff.getFieldDiff("lastContributor"));
        // contributors => different (update) / same / different (remove)
        ListPropertyDiff expectedListFieldDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedListFieldDiff.putDiff(0, new SimplePropertyDiff(PropertyType.STRING, "anotherAdministrator",
                "Administrator"));
        expectedListFieldDiff.putDiff(2, new SimplePropertyDiff(PropertyType.STRING, "jack", null));
        checkListFieldDiff(schemaDiff.getFieldDiff("contributors"), expectedListFieldDiff);
        // subjects => same / different (add)
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedListFieldDiff.putDiff(1, new SimplePropertyDiff(PropertyType.STRING, null, "Architecture"));
        checkListFieldDiff(schemaDiff.getFieldDiff("subjects"), expectedListFieldDiff);

        // ---------------------------
        // Check file schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "file", 2);

        // filename => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("filename"), PropertyType.STRING, "Jack.txt", "Joe.txt");
        // content => different
        ContentPropertyDiff expectedContentFieldDiff = new ContentPropertyDiff(DifferenceType.different);
        Blob leftBlob = (Blob) leftDoc.getPropertyValue("file:content");
        Blob rightBlob = (Blob) rightDoc.getPropertyValue("file:content");
        expectedContentFieldDiff.setLeftContent(new ContentProperty(null, null, "Jack.txt", getDigest(leftBlob)));
        expectedContentFieldDiff.setRightContent(new ContentProperty(null, null, "Joe.txt", getDigest(rightBlob)));
        checkContentFieldDiff(schemaDiff.getFieldDiff("content"), expectedContentFieldDiff);

        // ---------------------------
        // Check files schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "files", 1);

        // files =>
        // item1: same
        // item2: different filename
        // item3: different content
        // item4: add
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);

        ComplexPropertyDiff item1ExpectedComplexFieldDiff = new ComplexPropertyDiff();
        item1ExpectedComplexFieldDiff.putDiff("filename", new SimplePropertyDiff(PropertyType.STRING,
                "the_file_name_is_different.txt", "second_attachement.txt"));
        item1ExpectedComplexFieldDiff.putDiff("file", new ContentPropertyDiff(DifferenceType.differentFilename,
                new ContentProperty(null, null, "the_file_name_is_different.txt", null), new ContentProperty(null,
                        null, "second_attachement.txt", null)));

        ComplexPropertyDiff item2ExpectedComplexFieldDiff = new ComplexPropertyDiff();
        leftBlob = (Blob) leftDoc.getPropertyValue("files:files/2/file");
        rightBlob = (Blob) rightDoc.getPropertyValue("files:files/2/file");
        item2ExpectedComplexFieldDiff.putDiff("file", new ContentPropertyDiff(DifferenceType.differentDigest,
                new ContentProperty(null, null, null, getDigest(leftBlob)), new ContentProperty(null, null, null,
                        getDigest(rightBlob))));

        ComplexPropertyDiff item3ExpectedComplexFieldDiff = new ComplexPropertyDiff();
        item3ExpectedComplexFieldDiff.putDiff("filename", new SimplePropertyDiff(PropertyType.STRING, null,
                "fourth_attachement.txt"));
        rightBlob = (Blob) rightDoc.getPropertyValue("files:files/3/file");
        item3ExpectedComplexFieldDiff.putDiff("file", new ContentPropertyDiff(DifferenceType.different,
                new ContentProperty(), new ContentProperty("UTF-8", "text/plain", "fourth_attachement.txt",
                        getDigest(rightBlob))));

        expectedListFieldDiff.putDiff(1, item1ExpectedComplexFieldDiff);
        expectedListFieldDiff.putDiff(2, item2ExpectedComplexFieldDiff);
        expectedListFieldDiff.putDiff(3, item3ExpectedComplexFieldDiff);

        checkListFieldDiff(schemaDiff.getFieldDiff("files"), expectedListFieldDiff);

        // ---------------------------
        // Check simpletypes schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "simpletypes", 4);

        // string => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("string"), PropertyType.STRING, "a different string property",
                "a string property");
        // textarea => same
        checkIdenticalField(schemaDiff.getFieldDiff("textarea"));
        // boolean => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("boolean"), PropertyType.BOOLEAN, null,
                String.valueOf(Boolean.TRUE));
        // integer => same
        checkIdenticalField(schemaDiff.getFieldDiff("integer"));
        // date => same
        checkIdenticalField(schemaDiff.getFieldDiff("date"));
        // htmlText => different
        checkSimpleFieldDiff(
                schemaDiff.getFieldDiff("htmlText"),
                PropertyType.STRING,
                "&lt;p&gt;html  text modified with &lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;\n&lt;p&gt;&amp;nbsp;&lt;/p&gt;",
                "&lt;p&gt;html text with &lt;strong&gt;&lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/strong&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;");
        // multivalued => different (add) * 4
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedListFieldDiff.putDiff(0, new SimplePropertyDiff(PropertyType.STRING, null, "monday"));
        expectedListFieldDiff.putDiff(1, new SimplePropertyDiff(PropertyType.STRING, null, "tuesday"));
        expectedListFieldDiff.putDiff(2, new SimplePropertyDiff(PropertyType.STRING, null, "wednesday"));
        expectedListFieldDiff.putDiff(3, new SimplePropertyDiff(PropertyType.STRING, null, "thursday"));
        checkListFieldDiff(schemaDiff.getFieldDiff("multivalued"), expectedListFieldDiff);

        // ---------------------------
        // Check complextypes schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "complextypes", 2);

        // complex => same / different (update) / different (add) / different
        // (remove)
        ComplexPropertyDiff expectedComplexFieldDiff = new ComplexPropertyDiff();
        expectedComplexFieldDiff.putDiff(
                "booleanItem",
                new SimplePropertyDiff(PropertyType.BOOLEAN, String.valueOf(Boolean.FALSE),
                        String.valueOf(Boolean.TRUE)));
        expectedComplexFieldDiff.putDiff("integerItem", new SimplePropertyDiff(PropertyType.LONG, null, "10"));
        expectedComplexFieldDiff.putDiff("dateItem", new SimplePropertyDiff(PropertyType.DATE,
                "2011-12-29T23:00:00.00Z", null));
        checkComplexFieldDiff(schemaDiff.getFieldDiff("complex"), expectedComplexFieldDiff);

        // complexList =>
        // item1: same / different (update) / different (add) / different
        // (remove)
        // item2: remove
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);

        item1ExpectedComplexFieldDiff = new ComplexPropertyDiff();
        item1ExpectedComplexFieldDiff.putDiff(
                "booleanItem",
                new SimplePropertyDiff(PropertyType.BOOLEAN, String.valueOf(Boolean.FALSE),
                        String.valueOf(Boolean.TRUE)));
        item1ExpectedComplexFieldDiff.putDiff("integerItem", new SimplePropertyDiff(PropertyType.LONG, null, "12"));
        item1ExpectedComplexFieldDiff.putDiff("dateItem", new SimplePropertyDiff(PropertyType.DATE,
                "2011-12-30T23:00:00.00Z", null));

        item2ExpectedComplexFieldDiff = new ComplexPropertyDiff();
        item2ExpectedComplexFieldDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING,
                "second element of a complex list", null));
        item2ExpectedComplexFieldDiff.putDiff("booleanItem",
                new SimplePropertyDiff(PropertyType.BOOLEAN, String.valueOf(Boolean.FALSE), null));
        item2ExpectedComplexFieldDiff.putDiff("integerItem", new SimplePropertyDiff(PropertyType.LONG, "20", null));
        item2ExpectedComplexFieldDiff.putDiff("dateItem", new SimplePropertyDiff(PropertyType.DATE, "", null));

        expectedListFieldDiff.putDiff(0, item1ExpectedComplexFieldDiff);
        expectedListFieldDiff.putDiff(1, item2ExpectedComplexFieldDiff);

        checkListFieldDiff(schemaDiff.getFieldDiff("complexList"), expectedListFieldDiff);

        // ---------------------------
        // Check listoflists schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "listoflists", 1);

        // listOfLists =>
        // item1: same
        // item2: different
        // item3: add
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);

        item1ExpectedComplexFieldDiff = new ComplexPropertyDiff();
        item1ExpectedComplexFieldDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING,
                "second item is different", "second item"));
        ListPropertyDiff expectedNestedListDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedNestedListDiff.putDiff(1, new SimplePropertyDiff(PropertyType.STRING, "Friday", "Thursday"));
        expectedNestedListDiff.putDiff(2, new SimplePropertyDiff(PropertyType.STRING, "Saturday", null));
        expectedNestedListDiff.putDiff(3, new SimplePropertyDiff(PropertyType.STRING, "July", null));
        expectedNestedListDiff.putDiff(4, new SimplePropertyDiff(PropertyType.STRING, "August", null));
        item1ExpectedComplexFieldDiff.putDiff("stringListItem", expectedNestedListDiff);

        item2ExpectedComplexFieldDiff = new ComplexPropertyDiff();
        item2ExpectedComplexFieldDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, "third item",
                null));
        item2ExpectedComplexFieldDiff.putDiff("stringListItem", new ListPropertyDiff(PropertyType.SCALAR_LIST));

        expectedListFieldDiff.putDiff(1, item1ExpectedComplexFieldDiff);
        expectedListFieldDiff.putDiff(2, item2ExpectedComplexFieldDiff);

        checkListFieldDiff(schemaDiff.getFieldDiff("listOfLists"), expectedListFieldDiff);

    }

    protected final String getDigest(Blob blob) throws ClientException {
        try {
            Binary binary = binaryManager.getBinary(blob);
            if (binary != null) {
                return binary.getDigest();
            }
        } catch (IOException ioe) {
            throw new ClientException(String.format("Error while retrieving binary for blob '%s'.", blob.toString()),
                    ioe);
        }
        return null;
    }

}
