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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.diff.DiffTestCase;
import org.nuxeo.ecm.diff.model.DifferenceType;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.PropertyDiff;
import org.nuxeo.ecm.diff.model.PropertyType;
import org.nuxeo.ecm.diff.model.SchemaDiff;
import org.nuxeo.ecm.diff.model.impl.ComplexPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.ContentProperty;
import org.nuxeo.ecm.diff.model.impl.ContentPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.ListPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.SimplePropertyDiff;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests the {@link DocumentDiffService} on hand-made pieces of XML similar to document XML exports.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.diff.core" })
public class TestXMLDiff extends DiffTestCase {

    private static final Log LOGGER = LogFactory.getLog(TestXMLDiff.class);

    @Inject
    protected DocumentDiffService docDiffService;

    /**
     * Tests the diff configuration from the DocDiffService.
     *
     * @throws Exception the exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDiffConfiguration() throws Exception {

        String myControlXML = "<document>" + "<schema name=\"dublincore\"><title>joe</title></schema>"
                + "<schema name=\"uid\"><minor_version>0</minor_version><major_version>0</major_version></schema>"
                + "<schema name=\"note\"><note>myNote</note></schema>"
                + "<schema name=\"common\"><size>30</size></schema>" + "<schema name=\"emptySchema\"/>" + "</document>";
        String myTestXML = "<document>"
                + "<schema name=\"common\"><size>30</size></schema>"
                + "<schema name=\"dublincore\"><title>jack</title></schema>"
                + "<schema name=\"note\"><note/></schema>"
                + "<schema name=\"uid\"><minor_version>0</minor_version></schema>"
                + "<schema name=\"file\"><content type=\"content\"/><filename type=\"string\">test_file.doc</filename></schema>"
                + "</document>";

        // Configure XMLUnit
        docDiffService.configureXMLUnit();

        // Build diff
        Diff diff = new Diff(myControlXML, myTestXML);

        // Check diff
        assertFalse("Test XML and control XML should not be identical", diff.identical());
        assertFalse("Test XML and control XML should not be similar", diff.similar());

        // Configure diff
        docDiffService.configureDiff(diff);

        // Build detailed diff
        DetailedDiff detailedDiff = new DetailedDiff(diff);

        // Check detailed diff
        List<Difference> differences = detailedDiff.getAllDifferences();
        assertEquals("Wrong difference count", 3, differences.size());

        // Check 1st diff: TEXT_VALUE
        // joe --> jack
        Difference diff1 = differences.get(0);
        assertEquals("Wrong difference type", DifferenceConstants.TEXT_VALUE_ID, diff1.getId());

        // Check 2nd diff: CHILD_NODE_NOT_FOUND
        // <major_version> -->
        Difference diff2 = differences.get(1);
        assertEquals("Wrong difference type", DifferenceConstants.CHILD_NODE_NOT_FOUND_ID, diff2.getId());

        // Check 3d diff: HAS_CHILD_NODES
        // <note>myNote</note> --> <note/>
        Difference diff3 = differences.get(2);
        assertEquals("Wrong difference type", DifferenceConstants.HAS_CHILD_NODES_ID, diff3.getId());
    }

    /**
     * Tests schema diff. Schemas that are not shared by the 2 docs should not be considered as differences.
     *
     * @throws ClientException the client exception
     */
    @Test
    public void testSchemaDiff() throws ClientException {

        String leftXML = "<schema xmlns:dc=\"dcNS\" name=\"dublincore\"><dc:title type=\"string\">joe</dc:title></schema>"
                + "<schema name=\"uid\"><minor_version type=\"integer\">0</minor_version><major_version type=\"integer\">0</major_version></schema>";

        String rightXML = "<schema xmlns:dc=\"dcNS\" name=\"dublincore\"><dc:title type=\"string\">jack</dc:title></schema>"
                + "<schema name=\"file\"><content type=\"content\"/><filename type=\"string\">test_file.doc</filename></schema>";

        DocumentDiff docDiff = docDiffService.diff(wrapXMLIntoDocument(leftXML), wrapXMLIntoDocument(rightXML));

        SchemaDiff schemaDiff = checkSchemaDiff(docDiff, "dublincore", 1);
        PropertyDiff propertyDiff = schemaDiff.getFieldDiff("title");
        checkSimpleFieldDiff(propertyDiff, PropertyType.STRING, "joe", "jack");

        checkNullSchemaDiff(docDiff, "uid");
        checkNullSchemaDiff(docDiff, "file");

    }

    /**
     * Tests a TEXT_VALUE diff in a simple property.
     *
     * @throws ClientException the client exception
     */
    @Test
    public void testTextValueSimplePropertyDiff() throws ClientException {

        String leftXML = "<dc:title type=\"string\">joe</dc:title>";
        String rightXML = "<dc:title type=\"string\">jack</dc:title>";

        PropertyDiff propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "title");
        checkSimpleFieldDiff(propertyDiff, PropertyType.STRING, "joe", "jack");

        leftXML = "<major_version type=\"integer\">0</major_version>";
        rightXML = "<major_version type=\"integer\">1</major_version>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "major_version");
        checkSimpleFieldDiff(propertyDiff, PropertyType.INTEGER, "0", "1");

    }

    /**
     * Tests a TEXT_VALUE diff in a list property.
     *
     * @throws ClientException the client exception
     */
    @Test
    public void testTextValueListPropertyDiff() throws ClientException {

        // Simple list
        String leftXML = "<dc:contributors type=\"scalarList\"><item type=\"string\">joe</item><item type=\"string\">jack</item><item type=\"string\">bob</item></dc:contributors>";
        String rightXML = "<dc:contributors type=\"scalarList\"><item type=\"string\">john</item><item type=\"string\">jack</item><item type=\"string\">robert</item></dc:contributors>";

        PropertyDiff propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "contributors");

        ListPropertyDiff expectedFieldDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedFieldDiff.putDiff(0, new SimplePropertyDiff(PropertyType.STRING, "joe", "john"));
        expectedFieldDiff.putDiff(2, new SimplePropertyDiff(PropertyType.STRING, "bob", "robert"));

        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Simple list with missing node on the right side
        leftXML = "<dc:contributors type=\"scalarList\"><item type=\"string\">joe</item><item type=\"string\">bob</item></dc:contributors>";
        rightXML = "<dc:contributors type=\"scalarList\"><item type=\"string\">john</item></dc:contributors>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "contributors");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedFieldDiff.putDiff(0, new SimplePropertyDiff(PropertyType.STRING, "joe", "john"));
        expectedFieldDiff.putDiff(1, new SimplePropertyDiff(PropertyType.STRING, "bob", null));

        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list
        leftXML = "<dc:complexList type=\"complexList\">"
                + "<complexListItem type=\"complex\"><firstname type=\"string\">Antoine</firstname><age type=\"integer\">30</age></complexListItem>"
                + "</dc:complexList>";
        rightXML = "<dc:complexList type=\"complexList\">"
                + "<complexListItem type=\"complex\"><firstname type=\"string\">John</firstname><age type=\"integer\">40</age></complexListItem>"
                + "</dc:complexList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);
        ComplexPropertyDiff expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("firstname", new SimplePropertyDiff(PropertyType.STRING, "Antoine", "John"));
        expectedComplexPropDiff.putDiff("age", new SimplePropertyDiff(PropertyType.INTEGER, "30", "40"));
        expectedFieldDiff.putDiff(0, expectedComplexPropDiff);

        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list with missing nodes on the right side
        leftXML = "<dc:complexList type=\"complexList\">"
                + "<complexListItem type=\"complex\"><firstname type=\"string\">Antoine</firstname><lastname type=\"string\">Taillefer</lastname></complexListItem>"
                + "<complexListItem type=\"complex\"><firstname type=\"string\">John</firstname><lastname type=\"string\">Doe</lastname></complexListItem>"
                + "<complexListItem type=\"complex\"><firstname type=\"string\">Jimmy</firstname><lastname type=\"string\">Doe</lastname></complexListItem>"
                + "<complexListItem type=\"complex\"><firstname type=\"string\">Jack</firstname><lastname type=\"string\">Nicholson</lastname></complexListItem>"
                + "</dc:complexList>";
        rightXML = "<dc:complexList type=\"complexList\">"
                + "<complexListItem type=\"complex\"><firstname type=\"string\">Antoine</firstname><lastname type=\"string\">Taillefer</lastname></complexListItem>"
                + "<complexListItem type=\"complex\"><firstname type=\"string\">Bob</firstname><lastname type=\"string\">Doe</lastname></complexListItem>"
                + "</dc:complexList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);
        expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("firstname", new SimplePropertyDiff(PropertyType.STRING, "John", "Bob"));
        ComplexPropertyDiff expectedComplexPropDiff2 = new ComplexPropertyDiff();
        expectedComplexPropDiff2.putDiff("firstname", new SimplePropertyDiff(PropertyType.STRING, "Jimmy", null));
        expectedComplexPropDiff2.putDiff("lastname", new SimplePropertyDiff(PropertyType.STRING, "Doe", null));
        ComplexPropertyDiff expectedComplexPropDiff3 = new ComplexPropertyDiff();
        expectedComplexPropDiff3.putDiff("firstname", new SimplePropertyDiff(PropertyType.STRING, "Jack", null));
        expectedComplexPropDiff3.putDiff("lastname", new SimplePropertyDiff(PropertyType.STRING, "Nicholson", null));
        expectedFieldDiff.putDiff(1, expectedComplexPropDiff);
        expectedFieldDiff.putDiff(2, expectedComplexPropDiff2);
        expectedFieldDiff.putDiff(3, expectedComplexPropDiff3);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list with nested list with missing node on the right side
        leftXML = "<dc:complexList type=\"complexList\">"
                + "<complexListItem type=\"complex\"><listItem type=\"scalarList\"><item type=\"string\">joe</item><item type=\"string\">john</item></listItem></complexListItem>"
                + "</dc:complexList>";
        rightXML = "<dc:complexList type=\"complexList\">"
                + "<complexListItem type=\"complex\"><listItem type=\"scalarList\"><item type=\"string\">jack</item></listItem></complexListItem>"
                + "</dc:complexList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);
        expectedComplexPropDiff = new ComplexPropertyDiff();
        ListPropertyDiff expectedListPropDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedListPropDiff.putDiff(0, new SimplePropertyDiff(PropertyType.STRING, "joe", "jack"));
        expectedListPropDiff.putDiff(1, new SimplePropertyDiff(PropertyType.STRING, "john", null));
        expectedComplexPropDiff.putDiff("listItem", expectedListPropDiff);
        expectedFieldDiff.putDiff(0, expectedComplexPropDiff);

        // Complex list with a nested complex item
        leftXML = "<dc:files type=\"complexList\">"
                + "<item type=\"complex\"><filename type=\"string\">toto.txt</filename>"
                + "<file type=\"content\"><encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>toto.txt</filename><digest>5dafdabf966043c8c8cef20011e939a2</digest></file>"
                + "</item></dc:files>";
        rightXML = "<dc:files type=\"complexList\">"
                + "<item type=\"complex\"><filename type=\"string\">otherFile.pdf</filename>"
                + "<file type=\"content\"><encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>otherFile.pdf</filename><digest>e5340f8f697a4a916b3ca485a042a7dd</digest></file>"
                + "</item></dc:files>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "files");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);
        expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("filename", new SimplePropertyDiff(PropertyType.STRING, "toto.txt",
                "otherFile.pdf"));
        ContentPropertyDiff filePropDiff = new ContentPropertyDiff(DifferenceType.different, new ContentProperty(null,
                null, "toto.txt", "5dafdabf966043c8c8cef20011e939a2"), new ContentProperty(null, null, "otherFile.pdf",
                "e5340f8f697a4a916b3ca485a042a7dd"));
        expectedComplexPropDiff.putDiff("file", filePropDiff);
        expectedFieldDiff.putDiff(0, expectedComplexPropDiff);

        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Content list
        leftXML = "<dc:contentList type=\"contentList\">"
                + "<contentListItem type=\"content\"><encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>Joe.txt</filename><digest>5dafdabf966043c8c8cef20011e939a2</digest></contentListItem>"
                + "</dc:contentList>";
        rightXML = "<dc:contentList type=\"contentList\">"
                + "<contentListItem type=\"content\"><encoding>ISO-8859-1</encoding><mime-type>text/html</mime-type><filename>Jack.txt</filename><digest>e5340f8f697a4a916b3ca485a042a7dd</digest></contentListItem>"
                + "</dc:contentList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "contentList");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.CONTENT_LIST);
        ContentPropertyDiff expectedContentPropDiff = new ContentPropertyDiff(DifferenceType.different);
        expectedContentPropDiff.setLeftContent(new ContentProperty("UTF-8", "text/plain", "Joe.txt",
                "5dafdabf966043c8c8cef20011e939a2"));
        expectedContentPropDiff.setRightContent(new ContentProperty("ISO-8859-1", "text/html", "Jack.txt",
                "e5340f8f697a4a916b3ca485a042a7dd"));
        expectedFieldDiff.putDiff(0, expectedContentPropDiff);

        checkListFieldDiff(propertyDiff, expectedFieldDiff);
    }

    /**
     * Tests a TEXT_VALUE diff in a complex property.
     *
     * @throws ClientException the client exception
     */
    @Test
    public void testTextValueComplexPropertyDiff() throws ClientException {

        // Simple complex type
        String leftXML = "<dc:complex type=\"complex\"><stringItem type=\"string\">joe</stringItem><booleanItem type=\"boolean\">true</booleanItem></dc:complex>";
        String rightXML = "<dc:complex type=\"complex\"><stringItem type=\"string\">jack</stringItem><booleanItem type=\"boolean\">true</booleanItem></dc:complex>";

        PropertyDiff propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complex");

        ComplexPropertyDiff expectedFieldDiff = new ComplexPropertyDiff();
        expectedFieldDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, "joe", "jack"));
        checkComplexFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex type with a nested list item with missing node on the right
        // side
        leftXML = "<dc:complex type=\"complex\">"
                + "<listItem type=\"scalarList\"><item type=\"string\">joe</item><item type=\"string\">jack</item></listItem>"
                + "<booleanItem type=\"boolean\">true</booleanItem></dc:complex>";
        rightXML = "<dc:complex type=\"complex\">"
                + "<listItem type=\"scalarList\"><item type=\"string\">john</item></listItem>"
                + "<booleanItem type=\"boolean\">false</booleanItem></dc:complex>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complex");

        expectedFieldDiff = new ComplexPropertyDiff();
        ListPropertyDiff expectedListPropDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedListPropDiff.putDiff(0, new SimplePropertyDiff(PropertyType.STRING, "joe", "john"));
        expectedListPropDiff.putDiff(1, new SimplePropertyDiff(PropertyType.STRING, "jack", null));
        expectedFieldDiff.putDiff("listItem", expectedListPropDiff);
        expectedFieldDiff.putDiff("booleanItem", new SimplePropertyDiff(PropertyType.BOOLEAN, "true", "false"));

        checkComplexFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex type with a nested complex item
        leftXML = "<dc:complex type=\"complex\">"
                + "<complexItem type=\"complex\"><stringItem type=\"string\">joe</stringItem><integerItem type=\"integer\">10</integerItem></complexItem>"
                + "<booleanItem type=\"boolean\">true</booleanItem></dc:complex>";
        rightXML = "<dc:complex type=\"complex\">"
                + "<complexItem type=\"complex\"><stringItem type=\"string\">jack</stringItem><integerItem type=\"integer\">20</integerItem></complexItem>"
                + "<booleanItem type=\"boolean\">false</booleanItem></dc:complex>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complex");

        expectedFieldDiff = new ComplexPropertyDiff();
        ComplexPropertyDiff expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, "joe", "jack"));
        expectedComplexPropDiff.putDiff("integerItem", new SimplePropertyDiff(PropertyType.INTEGER, "10", "20"));
        expectedFieldDiff.putDiff("complexItem", expectedComplexPropDiff);
        expectedFieldDiff.putDiff("booleanItem", new SimplePropertyDiff(PropertyType.BOOLEAN, "true", "false"));

        checkComplexFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex type with a nested content item
        leftXML = "<dc:complex type=\"complex\">"
                + "<contentItem type=\"content\"><encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>Joe.txt</filename><digest>5dafdabf966043c8c8cef20011e939a2</digest></contentItem>"
                + "<stringItem type=\"string\">joe</stringItem>" + "</dc:complex>";
        rightXML = "<dc:complex type=\"complex\">"
                + "<contentItem type=\"content\"><encoding>ISO-8859-1</encoding><mime-type>text/html</mime-type><filename>Jack.txt</filename><digest>e5340f8f697a4a916b3ca485a042a7dd</digest></contentItem>"
                + "<stringItem type=\"string\">jack</stringItem>" + "</dc:complex>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complex");

        expectedFieldDiff = new ComplexPropertyDiff();
        expectedFieldDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, "joe", "jack"));
        ContentPropertyDiff expectedContentPropDiff = new ContentPropertyDiff(DifferenceType.different);
        expectedContentPropDiff.setLeftContent(new ContentProperty("UTF-8", "text/plain", "Joe.txt",
                "5dafdabf966043c8c8cef20011e939a2"));
        expectedContentPropDiff.setRightContent(new ContentProperty("ISO-8859-1", "text/html", "Jack.txt",
                "e5340f8f697a4a916b3ca485a042a7dd"));
        expectedFieldDiff.putDiff("contentItem", expectedContentPropDiff);

        checkComplexFieldDiff(propertyDiff, expectedFieldDiff);
    }

    /**
     * Tests a TEXT_VALUE diff in a content property.
     *
     * @throws ClientException the client exception
     */
    @Test
    public void testTextValueContentPropertyDiff() throws ClientException {

        // Different filename but same digest
        String leftXML = "<dc:content type=\"content\"><encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>test_joe.txt</filename><digest>5dafdabf966043c8c8cef20011e939a2</digest></dc:content>";
        String rightXML = "<dc:content type=\"content\"><encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>test_jack.txt</filename><digest>5dafdabf966043c8c8cef20011e939a2</digest></dc:content>";

        PropertyDiff propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "content");

        ContentPropertyDiff expectedFieldDiff = new ContentPropertyDiff(DifferenceType.differentFilename);
        expectedFieldDiff.setLeftContent(new ContentProperty(null, null, "test_joe.txt", null));
        expectedFieldDiff.setRightContent(new ContentProperty(null, null, "test_jack.txt", null));
        checkContentFieldDiff(propertyDiff, expectedFieldDiff);

        // Different digest but same filename
        leftXML = "<dc:content type=\"content\"><encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>test_joe.txt</filename><digest>5dafdabf966043c8c8cef20011e939a2</digest></dc:content>";
        rightXML = "<dc:content type=\"content\"><encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>test_joe.txt</filename><digest>e5340f8f697a4a916b3ca485a042a7dd</digest></dc:content>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "content");

        expectedFieldDiff = new ContentPropertyDiff(DifferenceType.differentDigest);
        expectedFieldDiff.setLeftContent(new ContentProperty(null, null, null, "5dafdabf966043c8c8cef20011e939a2"));
        expectedFieldDiff.setRightContent(new ContentProperty(null, null, null, "e5340f8f697a4a916b3ca485a042a7dd"));
        checkContentFieldDiff(propertyDiff, expectedFieldDiff);

        // Different digest and filename
        leftXML = "<dc:content type=\"content\"><encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>test_joe.txt</filename><digest>5dafdabf966043c8c8cef20011e939a2</digest></dc:content>";
        rightXML = "<dc:content type=\"content\"><encoding>ISO-8859-1</encoding><mime-type>text/html</mime-type><filename>test_jack.txt</filename><digest>e5340f8f697a4a916b3ca485a042a7dd</digest></dc:content>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "content");

        expectedFieldDiff = new ContentPropertyDiff(DifferenceType.different);
        expectedFieldDiff.setLeftContent(new ContentProperty("UTF-8", "text/plain", "test_joe.txt",
                "5dafdabf966043c8c8cef20011e939a2"));
        expectedFieldDiff.setRightContent(new ContentProperty("ISO-8859-1", "text/html", "test_jack.txt",
                "e5340f8f697a4a916b3ca485a042a7dd"));
        checkContentFieldDiff(propertyDiff, expectedFieldDiff);

        // Same digest and filename but different encoding or mime type
        leftXML = "<dc:content type=\"content\"><encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>test_joe.txt</filename><digest>5dafdabf966043c8c8cef20011e939a2</digest></dc:content>";
        rightXML = "<dc:content type=\"content\"><encoding>ISO-8859-1</encoding><mime-type>text/html</mime-type><filename>test_joe.txt</filename><digest>5dafdabf966043c8c8cef20011e939a2</digest></dc:content>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "content");

        expectedFieldDiff = new ContentPropertyDiff(DifferenceType.different);
        expectedFieldDiff.setLeftContent(new ContentProperty("UTF-8", "text/plain", null, null));
        expectedFieldDiff.setRightContent(new ContentProperty("ISO-8859-1", "text/html", null, null));
        checkContentFieldDiff(propertyDiff, expectedFieldDiff);

        // Strict equality
        leftXML = "<dc:content type=\"content\">"
                + "<encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>test_joe.txt</filename><digest>5dafdabf966043c8c8cef20011e939a2</digest><data>94aa8925.blob</data>"
                + "</dc:content>";
        rightXML = "<dc:content type=\"content\">"
                + "<encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>test_joe.txt</filename><digest>5dafdabf966043c8c8cef20011e939a2</digest><data>8ce007a5.blob</data>"
                + "</dc:content>";

        DocumentDiff docDiff = docDiffService.diff(wrapXMLIntoSchema(leftXML), wrapXMLIntoSchema(rightXML));
        checkNullSchemaDiff(docDiff, "dublincore");
    }

    // No need to test a CHILD_NODE_NOT_FOUND diff in a simple property.

    /**
     * Tests CHILD_NODE_NOT_FOUND diff in a list type property.
     *
     * @throws ClientException the client exception
     */
    @Test
    public void testChildNodeNotFoundListPropertyDiff() throws ClientException {

        // Simple list
        String leftXML = "<dc:contributors type=\"scalarList\"><item type=\"string\">joe</item></dc:contributors>";
        String rightXML = "<dc:contributors type=\"scalarList\"><item type=\"string\">joe</item><item type=\"string\">jack</item></dc:contributors>";

        PropertyDiff propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "contributors");

        ListPropertyDiff expectedFieldDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedFieldDiff.putDiff(1, new SimplePropertyDiff(PropertyType.STRING, null, "jack"));
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list
        leftXML = "<dc:complexList type=\"complexList\">"
                + "<complexItem type=\"complex\"><firstname type=\"string\">Antoine</firstname><lastname type=\"string\">Taillefer</lastname></complexItem>"
                + "</dc:complexList>";
        rightXML = "<dc:complexList type=\"complexList\">"
                + "<complexItem type=\"complex\"><firstname type=\"string\">Antoine</firstname><lastname type=\"string\">Taillefer</lastname></complexItem>"
                + "<complexItem type=\"complex\"><firstname type=\"string\">John</firstname><lastname type=\"string\">Doe</lastname></complexItem>"
                + "<complexItem type=\"complex\"><firstname type=\"string\">Jack</firstname><lastname type=\"string\">Nicholson</lastname></complexItem>"
                + "</dc:complexList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);
        ComplexPropertyDiff expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("firstname", new SimplePropertyDiff(PropertyType.STRING, null, "John"));
        expectedComplexPropDiff.putDiff("lastname", new SimplePropertyDiff(PropertyType.STRING, null, "Doe"));
        ComplexPropertyDiff expectedComplexPropDiff2 = new ComplexPropertyDiff();
        expectedComplexPropDiff2.putDiff("firstname", new SimplePropertyDiff(PropertyType.STRING, null, "Jack"));
        expectedComplexPropDiff2.putDiff("lastname", new SimplePropertyDiff(PropertyType.STRING, null, "Nicholson"));
        expectedFieldDiff.putDiff(1, expectedComplexPropDiff);
        expectedFieldDiff.putDiff(2, expectedComplexPropDiff2);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list with a nested list item
        leftXML = "<dc:listOfList type=\"complexList\">"
                + "<complexItem type=\"complex\">"
                + "<listItem type=\"scalarList\"><item type=\"string\">Monday</item><item type=\"string\">Tuesday</item></listItem>"
                + "<stringItem type=\"string\">bob</stringItem>" + "</complexItem></dc:listOfList>";
        rightXML = "<dc:listOfList type=\"complexList\">"
                + "<complexItem type=\"complex\">"
                + "<listItem type=\"scalarList\"><item type=\"string\">Monday</item><item type=\"string\">Tuesday</item></listItem>"
                + "<stringItem type=\"string\">joe</stringItem></complexItem>"
                + "<complexItem type=\"complex\">"
                + "<listItem type=\"scalarList\"><item type=\"string\">Wednesday</item><item type=\"string\">Thursday</item></listItem>"
                + "<stringItem type=\"string\">jack</stringItem></complexItem>" + "</dc:listOfList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "listOfList");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);
        expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, "bob", "joe"));
        expectedComplexPropDiff2 = new ComplexPropertyDiff();
        expectedComplexPropDiff2.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, null, "jack"));
        ListPropertyDiff expectedNestedListPropDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedNestedListPropDiff.putDiff(0, new SimplePropertyDiff(PropertyType.STRING, null, "Wednesday"));
        expectedNestedListPropDiff.putDiff(1, new SimplePropertyDiff(PropertyType.STRING, null, "Thursday"));
        expectedComplexPropDiff2.putDiff("listItem", expectedNestedListPropDiff);
        expectedFieldDiff.putDiff(0, expectedComplexPropDiff);
        expectedFieldDiff.putDiff(1, expectedComplexPropDiff2);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);
    }

    /**
     * Tests CHILD_NODE_NOT_FOUND diff in a complex type property.
     *
     * @throws ClientException the client exception
     */
    @Test
    public void testChildNodeNotFoundComplexPropertyDiff() throws ClientException {

        // Complex type => should never happen
        String leftXML = "<dc:complexType type=\"complex\"><stringItem type=\"string\">joe</stringItem><booleanItem type=\"boolean\">true</booleanItem></dc:complexType>";
        String rightXML = "<dc:complexType type=\"complex\"><stringItem type=\"string\">joe</stringItem></dc:complexType>";

        try {
            getPropertyDiff(leftXML, rightXML, 1, "complexType");
            fail("A CHILD_NODE_NOT_FOUND difference should never be found within a complex type.");
        } catch (ClientException ce) {
            LOGGER.debug("Exception thrown as expected: " + ce.getMessage());
        }

    }

    /**
     * Tests CHILD_NODE_NOT_FOUND diff in a content type property.
     *
     * @throws ClientException the client exception
     */
    @Test
    public void testChildNodeNotFoundContentPropertyDiff() throws ClientException {

        // Complex type => should never happen
        String leftXML = "<dc:content type=\"content\"><encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>Joe.txt</filename><digest>5dafdabf966043c8c8cef20011e939a2</digest></dc:content>";
        String rightXML = "<dc:content type=\"content\"><encoding>UTF-8</encoding><mime-type>text/plain</mime-type></dc:content>";

        try {
            getPropertyDiff(leftXML, rightXML, 1, "content");
            fail("A CHILD_NODE_NOT_FOUND difference should never be found within a content type.");
        } catch (ClientException ce) {
            LOGGER.debug("Exception thrown as expected: " + ce.getMessage());
        }

    }

    /**
     * Tests HAS_CHILD_NODES diff in a simple property.
     *
     * @throws ClientException the client exception
     */
    @Test
    public void testHasChildNodesSimplePropertyDiff() throws ClientException {

        String leftXML = "<dc:title type=\"string\">joe</dc:title>";
        String rightXML = "<dc:title type=\"string\"/>";

        PropertyDiff propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "title");

        checkSimpleFieldDiff(propertyDiff, PropertyType.STRING, "joe", null);
    }

    /**
     * Tests HAS_CHILD_NODES diff in a list property.
     *
     * @throws ClientException the client exception
     */
    @Test
    public void testHasChildNodesListPropertyDiff() throws ClientException {

        // Simple list (no child nodes on the right side)
        String leftXML = "<dc:contributors type=\"scalarList\"><item type=\"string\">joe</item><item type=\"string\">jack</item></dc:contributors>";
        String rightXML = "<dc:contributors type=\"scalarList\"/>";

        PropertyDiff propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "contributors");

        ListPropertyDiff expectedFieldDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedFieldDiff.putDiff(0, new SimplePropertyDiff(PropertyType.STRING, "joe", null));
        expectedFieldDiff.putDiff(1, new SimplePropertyDiff(PropertyType.STRING, "jack", null));
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Simple list (no child nodes on the left side)
        leftXML = "<dc:contributors type=\"scalarList\"/>";
        rightXML = "<dc:contributors type=\"scalarList\"><item type=\"string\">joe</item><item type=\"string\">jack</item></dc:contributors>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "contributors");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedFieldDiff.putDiff(0, new SimplePropertyDiff(PropertyType.STRING, null, "joe"));
        expectedFieldDiff.putDiff(1, new SimplePropertyDiff(PropertyType.STRING, null, "jack"));
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // List (item with no child nodes on the right side)
        leftXML = "<dc:contributors type=\"scalarList\"><item type=\"string\">joe</item><item type=\"string\">jack</item></dc:contributors>";
        rightXML = "<dc:contributors type=\"scalarList\"><item type=\"string\">joe</item><item type=\"string\"/></dc:contributors>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "contributors");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedFieldDiff.putDiff(1, new SimplePropertyDiff(PropertyType.STRING, "jack", null));
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // List (item with no child nodes on the left side)
        leftXML = "<dc:contributors type=\"scalarList\"><item type=\"string\">joe</item><item type=\"string\"/></dc:contributors>";
        rightXML = "<dc:contributors type=\"scalarList\"><item type=\"string\">joe</item><item type=\"string\">jack</item></dc:contributors>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "contributors");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedFieldDiff.putDiff(1, new SimplePropertyDiff(PropertyType.STRING, null, "jack"));
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list (empty list on the right side)
        leftXML = "<dc:complexList type=\"complexList\">"
                + "<complexItem type=\"complex\"><stringItem type=\"string\">joe</stringItem><integerItem type=\"integer\">10</integerItem></complexItem>"
                + "</dc:complexList>";
        rightXML = "<dc:complexList type=\"complexList\"/>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);
        ComplexPropertyDiff expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, "joe", null));
        expectedComplexPropDiff.putDiff("integerItem", new SimplePropertyDiff(PropertyType.INTEGER, "10", null));
        expectedFieldDiff.putDiff(0, expectedComplexPropDiff);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list (empty list on the left side)
        leftXML = "<dc:complexList type=\"complexList\"/>";
        rightXML = "<dc:complexList type=\"complexList\">"
                + "<complexItem type=\"complex\"><stringItem type=\"string\">joe</stringItem><integerItem type=\"integer\">10</integerItem></complexItem>"
                + "</dc:complexList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);
        expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, null, "joe"));
        expectedComplexPropDiff.putDiff("integerItem", new SimplePropertyDiff(PropertyType.INTEGER, null, "10"));
        expectedFieldDiff.putDiff(0, expectedComplexPropDiff);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list (complex item with no child nodes on the right side)
        leftXML = "<dc:complexList type=\"complexList\">"
                + "<complexItem type=\"complex\"><stringItem type=\"string\">joe</stringItem><integerItem type=\"integer\">10</integerItem></complexItem>"
                + "</dc:complexList>";
        rightXML = "<dc:complexList type=\"complexList\">"
                + "<complexItem type=\"complex\"><stringItem type=\"string\">jack</stringItem><integerItem type=\"integer\"/></complexItem>"
                + "</dc:complexList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);
        expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, "joe", "jack"));
        expectedComplexPropDiff.putDiff("integerItem", new SimplePropertyDiff(PropertyType.INTEGER, "10", null));
        expectedFieldDiff.putDiff(0, expectedComplexPropDiff);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list (complex item with no child nodes on the left side)
        leftXML = "<dc:complexList type=\"complexList\">"
                + "<complexItem type=\"complex\"><stringItem type=\"string\">joe</stringItem><integerItem type=\"integer\"/></complexItem>"
                + "</dc:complexList>";
        rightXML = "<dc:complexList type=\"complexList\">"
                + "<complexItem type=\"complex\"><stringItem type=\"string\">jack</stringItem><integerItem type=\"integer\">10</integerItem></complexItem>"
                + "</dc:complexList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);
        expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, "joe", "jack"));
        expectedComplexPropDiff.putDiff("integerItem", new SimplePropertyDiff(PropertyType.INTEGER, null, "10"));
        expectedFieldDiff.putDiff(0, expectedComplexPropDiff);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list (complex item with no child nodes on the left side and
        // empty list on the right side)
        leftXML = "<dc:complexList type=\"complexList\">"
                + "<complexItem type=\"complex\"><stringItem type=\"string\">joe</stringItem><integerItem type=\"integer\"/></complexItem>"
                + "</dc:complexList>";
        rightXML = "<dc:complexList type=\"complexList\"/>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);
        expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, "joe", null));
        expectedComplexPropDiff.putDiff("integerItem", new SimplePropertyDiff(PropertyType.INTEGER, "", null));
        expectedFieldDiff.putDiff(0, expectedComplexPropDiff);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list (complex item with no child nodes on the right side and
        // empty list on the left side)
        leftXML = "<dc:complexList type=\"complexList\"/>";
        rightXML = "<dc:complexList type=\"complexList\">"
                + "<complexItem type=\"complex\"><stringItem type=\"string\">joe</stringItem><integerItem type=\"integer\"/></complexItem>"
                + "</dc:complexList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);
        expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, null, "joe"));
        expectedComplexPropDiff.putDiff("integerItem", new SimplePropertyDiff(PropertyType.INTEGER, null, ""));
        expectedFieldDiff.putDiff(0, expectedComplexPropDiff);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list with a content complex item (empty list on the right
        // side)
        leftXML = "<dc:files type=\"complexList\"><item type=\"complex\">"
                + "<filename type=\"string\">Joe.txt</filename>"
                + "<file type=\"content\"><encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>Joe.txt</filename><digest>5dafdabf966043c8c8cef20011e939a2</digest></file>"
                + "</item></dc:files>";
        rightXML = "<dc:files type=\"complexList\"/>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "files");

        expectedFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);
        expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("filename", new SimplePropertyDiff(PropertyType.STRING, "Joe.txt", null));
        expectedComplexPropDiff.putDiff("file", new ContentPropertyDiff(DifferenceType.different, new ContentProperty(
                "UTF-8", "text/plain", "Joe.txt", "5dafdabf966043c8c8cef20011e939a2"), new ContentProperty()));
        expectedFieldDiff.putDiff(0, expectedComplexPropDiff);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);
    }

    /**
     * Tests HAS_CHILD_NODES diff in a complex property.
     *
     * @throws ClientException the client exception
     */
    @Test
    public void testHasChildNodesComplexPropertyDiff() throws ClientException {

        // Simple complex type
        String leftXML = "<dc:complexType type=\"complex\"><stringItem type=\"string\">joe</stringItem><booleanItem type=\"boolean\">true</booleanItem></dc:complexType>";
        String rightXML = "<dc:complexType type=\"complex\"/>";

        PropertyDiff propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexType");

        ComplexPropertyDiff expectedFieldDiff = new ComplexPropertyDiff();
        expectedFieldDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, "joe", null));
        expectedFieldDiff.putDiff("booleanItem", new SimplePropertyDiff(PropertyType.BOOLEAN, "true", null));
        checkComplexFieldDiff(propertyDiff, expectedFieldDiff);

        // Simple complex type (item with no child nodes)
        leftXML = "<dc:complexType type=\"complex\"><stringItem type=\"string\">joe</stringItem><booleanItem type=\"boolean\">true</booleanItem></dc:complexType>";
        rightXML = "<dc:complexType type=\"complex\"><stringItem type=\"string\">joe</stringItem><booleanItem type=\"boolean\"/></dc:complexType>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexType");

        expectedFieldDiff = new ComplexPropertyDiff();
        expectedFieldDiff.putDiff("booleanItem", new SimplePropertyDiff(PropertyType.BOOLEAN, "true", null));
        checkComplexFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex type with a nested list item
        leftXML = "<dc:complexType type=\"complex\">"
                + "<stringItem type=\"string\">joe</stringItem>"
                + "<listItem type=\"scalarList\"><item type=\"string\">Monday</item><item type=\"string\">Tuesday</item></listItem>"
                + "</dc:complexType>";
        rightXML = "<dc:complexType type=\"complex\">"
                + "<stringItem type=\"string\">jack</stringItem><listItem type=\"scalarList\"/></dc:complexType>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexType");

        expectedFieldDiff = new ComplexPropertyDiff();
        ListPropertyDiff expectedListPropertyDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedListPropertyDiff.putDiff(0, new SimplePropertyDiff(PropertyType.STRING, "Monday", null));
        expectedListPropertyDiff.putDiff(1, new SimplePropertyDiff(PropertyType.STRING, "Tuesday", null));
        expectedFieldDiff.putDiff("listItem", expectedListPropertyDiff);
        expectedFieldDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, "joe", "jack"));
        checkComplexFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex type with a nested content item
        leftXML = "<dc:complexType type=\"complex\">"
                + "<stringItem type=\"string\">joe</stringItem>"
                + "<contentItem type=\"content\"><encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>Joe.txt</filename><digest>5dafdabf966043c8c8cef20011e939a2</digest></contentItem>"
                + "</dc:complexType>";
        rightXML = "<dc:complexType type=\"complex\">" + "<stringItem type=\"string\">jack</stringItem>"
                + "<contentItem type=\"content\"/>" + "</dc:complexType>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexType");

        expectedFieldDiff = new ComplexPropertyDiff();
        expectedFieldDiff.putDiff("stringItem", new SimplePropertyDiff(PropertyType.STRING, "joe", "jack"));
        ContentPropertyDiff expectedContentPropertyDiff = new ContentPropertyDiff(DifferenceType.different,
                new ContentProperty("UTF-8", "text/plain", "Joe.txt", "5dafdabf966043c8c8cef20011e939a2"),
                new ContentProperty());
        expectedFieldDiff.putDiff("contentItem", expectedContentPropertyDiff);
        checkComplexFieldDiff(propertyDiff, expectedFieldDiff);
    }

    @Test
    public void testHasChildNodesContentPropertyDiff() throws ClientException {

        // Empty content property
        String leftXML = "<dc:file type=\"content\">"
                + "<encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>Joe.txt</filename><digest>5dafdabf966043c8c8cef20011e939a2</digest>"
                + "</dc:file>";
        String rightXML = "<dc:file type=\"content\"/>";

        PropertyDiff propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "file");

        ContentPropertyDiff expectedFieldDiff = new ContentPropertyDiff(DifferenceType.different);
        expectedFieldDiff.setLeftContent(new ContentProperty("UTF-8", "text/plain", "Joe.txt",
                "5dafdabf966043c8c8cef20011e939a2"));
        expectedFieldDiff.setRightContent(new ContentProperty());
        checkContentFieldDiff(propertyDiff, expectedFieldDiff);

        // Empty content sub property
        leftXML = "<dc:file type=\"content\">"
                + "<encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>Joe.txt</filename><digest>5dafdabf966043c8c8cef20011e939a2</digest>"
                + "</dc:file>";
        rightXML = "<dc:file type=\"content\">"
                + "<encoding/><mime-type>text/html</mime-type><filename>Joe.txt</filename><digest/>" + "</dc:file>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "file");

        expectedFieldDiff = new ContentPropertyDiff(DifferenceType.differentDigest);
        expectedFieldDiff.setLeftContent(new ContentProperty("UTF-8", "text/plain", null,
                "5dafdabf966043c8c8cef20011e939a2"));
        expectedFieldDiff.setRightContent(new ContentProperty(null, "text/html", null, null));
        checkContentFieldDiff(propertyDiff, expectedFieldDiff);
    }

    /**
     * Wraps xml string in a document element.
     *
     * @param xml the xml
     * @return the string
     */
    protected final String wrapXMLIntoDocument(String xml) {

        StringBuilder sb = new StringBuilder("<document>");
        sb.append(xml);
        sb.append("</document>");

        return sb.toString();

    }

    /**
     * Wraps xml string in a schema element.
     *
     * @param xml the xml
     * @return the string
     */
    protected final String wrapXMLIntoSchema(String xml) {

        StringBuilder sb = new StringBuilder("<schema xmlns:dc=\"dcNS\" name= \"dublincore\">");
        sb.append(xml);
        sb.append("</schema>");

        return sb.toString();

    }

    /**
     * Gets the property diff.
     *
     * @param leftXML the left xml
     * @param rightXML the right xml
     * @param diffCount the diff count
     * @param field the field
     * @return the property diff
     * @throws ClientException the client exception
     */
    protected final PropertyDiff getPropertyDiff(String leftXML, String rightXML, int diffCount, String field)
            throws ClientException {

        DocumentDiff docDiff = docDiffService.diff(wrapXMLIntoSchema(leftXML), wrapXMLIntoSchema(rightXML));
        SchemaDiff schemaDiff = checkSchemaDiff(docDiff, "dublincore", diffCount);

        return schemaDiff.getFieldDiff(field);
    }

}
