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
package org.nuxeo.ecm.platform.diff;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.diff.helpers.DiffTestCase;
import org.nuxeo.ecm.platform.diff.model.DocumentDiff;
import org.nuxeo.ecm.platform.diff.model.PropertyDiff;
import org.nuxeo.ecm.platform.diff.model.SchemaDiff;
import org.nuxeo.ecm.platform.diff.model.impl.ComplexPropertyDiff;
import org.nuxeo.ecm.platform.diff.model.impl.ListPropertyDiff;
import org.nuxeo.ecm.platform.diff.model.impl.SimplePropertyDiff;
import org.nuxeo.ecm.platform.diff.service.DocumentDiffService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * Tests XML diff using DocumentDiffService.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.platform.diff" })
public class TestXMLDiff extends DiffTestCase {

    private static final Log LOGGER = LogFactory.getLog(TestXMLDiff.class);

    @Inject
    protected DocumentDiffService docDiffService;

    /**
     * Tests a TEXT_VALUE diff in a simple property.
     * 
     * @throws ClientException the client exception
     */
    @Test
    public void testTextValueSimplePropertyDiff() throws ClientException {

        String leftXML = "<dc:title>joe</dc:title>";
        String rightXML = "<dc:title>jack</dc:title>";

        PropertyDiff propertyDiff = getPropertyDiff(leftXML, rightXML, 1,
                "title");
        checkSimpleFieldDiff(propertyDiff, "joe", "jack");
    }

    /**
     * Tests a TEXT_VALUE diff in a list property.
     * 
     * @throws ClientException the client exception
     */
    @Test
    public void testTextValueListPropertyDiff() throws ClientException {

        // Simple list
        String leftXML = "<dc:contributors><item>joe</item><item>jack</item><item>bob</item></dc:contributors>";
        String rightXML = "<dc:contributors><item>john</item><item>jack</item><item>robert</item></dc:contributors>";

        PropertyDiff propertyDiff = getPropertyDiff(leftXML, rightXML, 1,
                "contributors");

        ListPropertyDiff expectedFieldDiff = new ListPropertyDiff();
        expectedFieldDiff.addDiff(new SimplePropertyDiff("joe", "john"));
        expectedFieldDiff.addDiff(new SimplePropertyDiff("bob", "robert"));

        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Simple list with missing node on the right side
        leftXML = "<dc:contributors><item>joe</item><item>bob</item></dc:contributors>";
        rightXML = "<dc:contributors><item>john</item></dc:contributors>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "contributors");

        expectedFieldDiff = new ListPropertyDiff();
        expectedFieldDiff.addDiff(new SimplePropertyDiff("joe", "john"));
        expectedFieldDiff.addDiff(new SimplePropertyDiff("bob", null));

        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // List of list
        leftXML = "<dc:listOfList>"
                + "<item><listItem><subListItem>Monday</subListItem><subListItem>Tuesday</subListItem></listItem></item>"
                + "<item><listItem><subListItem>Wednesday</subListItem><subListItem>Thursday</subListItem></listItem></item>"
                + "</dc:listOfList>";
        rightXML = "<dc:listOfList>"
                + "<item><listItem><subListItem>Saturday</subListItem><subListItem>Sunday</subListItem></listItem></item>"
                + "<item><listItem><subListItem>Friday</subListItem><subListItem>Thursday</subListItem></listItem></item>"
                + "</dc:listOfList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "listOfList");

        expectedFieldDiff = new ListPropertyDiff();
        ListPropertyDiff expectedListPropDiff = new ListPropertyDiff();
        expectedListPropDiff.addDiff(new SimplePropertyDiff("Monday",
                "Saturday"));
        expectedListPropDiff.addDiff(new SimplePropertyDiff("Tuesday", "Sunday"));
        ListPropertyDiff expectedListPropDiff2 = new ListPropertyDiff();
        expectedListPropDiff2 = new ListPropertyDiff();
        expectedListPropDiff2.addDiff(new SimplePropertyDiff("Wednesday",
                "Friday"));
        expectedFieldDiff.addDiff(expectedListPropDiff);
        expectedFieldDiff.addDiff(expectedListPropDiff2);

        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // List of list with missing node on the right side
        leftXML = "<dc:listOfList>"
                + "<item><listItem><subListItem>Monday</subListItem><subListItem>Tuesday</subListItem></listItem></item>"
                + "<item><listItem><subListItem>Wednesday</subListItem><subListItem>Thursday</subListItem></listItem></item>"
                + "</dc:listOfList>";
        rightXML = "<dc:listOfList>"
                + "<item><listItem><subListItem>Saturday</subListItem><subListItem>Sunday</subListItem></listItem></item>"
                + "</dc:listOfList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "listOfList");

        expectedFieldDiff = new ListPropertyDiff();
        expectedListPropDiff = new ListPropertyDiff();
        expectedListPropDiff.addDiff(new SimplePropertyDiff("Monday",
                "Saturday"));
        expectedListPropDiff.addDiff(new SimplePropertyDiff("Tuesday", "Sunday"));
        expectedListPropDiff2 = new ListPropertyDiff();
        expectedListPropDiff2.addDiff(new SimplePropertyDiff("Wednesday", null));
        expectedListPropDiff2.addDiff(new SimplePropertyDiff("Thursday", null));
        expectedFieldDiff.addDiff(expectedListPropDiff);
        expectedFieldDiff.addDiff(expectedListPropDiff2);

        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // List of list with nested missing node on the right side
        leftXML = "<dc:listOfList>"
                + "<item><listItem><subListItem>Monday</subListItem><subListItem>Tuesday</subListItem></listItem></item>"
                + "<item><listItem><subListItem>Wednesday</subListItem><subListItem>Thursday</subListItem></listItem></item>"
                + "</dc:listOfList>";
        rightXML = "<dc:listOfList>"
                + "<item><listItem><subListItem>Saturday</subListItem></listItem></item>"
                + "<item><listItem><subListItem>Friday</subListItem></listItem></item>"
                + "</dc:listOfList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "listOfList");

        expectedFieldDiff = new ListPropertyDiff();
        expectedListPropDiff = new ListPropertyDiff();
        expectedListPropDiff.addDiff(new SimplePropertyDiff("Monday",
                "Saturday"));
        expectedListPropDiff.addDiff(new SimplePropertyDiff("Tuesday", null));
        expectedListPropDiff2 = new ListPropertyDiff();
        expectedListPropDiff2 = new ListPropertyDiff();
        expectedListPropDiff2.addDiff(new SimplePropertyDiff("Wednesday",
                "Friday"));
        expectedListPropDiff2.addDiff(new SimplePropertyDiff("Thursday", null));
        expectedFieldDiff.addDiff(expectedListPropDiff);
        expectedFieldDiff.addDiff(expectedListPropDiff2);

        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list
        leftXML = "<dc:complexList>"
                + "<complexListItem><firstname>Antoine</firstname><lastname>Taillefer</lastname></complexListItem>"
                + "</dc:complexList>";
        rightXML = "<dc:complexList>"
                + "<complexListItem><firstname>John</firstname><lastname>Doe</lastname></complexListItem>"
                + "</dc:complexList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff();
        ComplexPropertyDiff expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("firstname", new SimplePropertyDiff(
                "Antoine", "John"));
        expectedComplexPropDiff.putDiff("lastname", new SimplePropertyDiff(
                "Taillefer", "Doe"));
        expectedFieldDiff.addDiff(expectedComplexPropDiff);

        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list with missing nodes on the right side
        leftXML = "<dc:complexList>"
                + "<complexListItem><firstname>Antoine</firstname><lastname>Taillefer</lastname></complexListItem>"
                + "<complexListItem><firstname>John</firstname><lastname>Doe</lastname></complexListItem>"
                + "<complexListItem><firstname>Jimmy</firstname><lastname>Page</lastname></complexListItem>"
                + "<complexListItem><firstname>Jack</firstname><lastname>Nicholson</lastname></complexListItem>"
                + "</dc:complexList>";
        rightXML = "<dc:complexList>"
                + "<complexListItem><firstname>Antoine</firstname><lastname>Taillefer</lastname></complexListItem>"
                + "<complexListItem><firstname>Bob</firstname><lastname>Plant</lastname></complexListItem>"
                + "</dc:complexList>";

        // propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff();
        expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("firstname", new SimplePropertyDiff(
                "John", "Bob"));
        expectedComplexPropDiff.putDiff("lastname", new SimplePropertyDiff(
                "Doe", "Plant"));
        ComplexPropertyDiff expectedComplexPropDiff2 = new ComplexPropertyDiff();
        expectedComplexPropDiff2.putDiff("firstname", new SimplePropertyDiff(
                "Jimmy", null));
        expectedComplexPropDiff2.putDiff("lastname", new SimplePropertyDiff(
                "Page", null));
        ComplexPropertyDiff expectedComplexPropDiff3 = new ComplexPropertyDiff();
        expectedComplexPropDiff3.putDiff("firstname", new SimplePropertyDiff(
                "Jack", null));
        expectedComplexPropDiff3.putDiff("lastname", new SimplePropertyDiff(
                "Nicholson", null));
        expectedFieldDiff.addDiff(expectedComplexPropDiff);
        expectedFieldDiff.addDiff(expectedComplexPropDiff2);
        expectedFieldDiff.addDiff(expectedComplexPropDiff3);
        // checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list with nested list with missing node on the right side
        leftXML = "<dc:complexList>"
                + "<complexListItem><listItem><item>joe</item><item>john</item></listItem></complexListItem>"
                + "</dc:complexList>";
        rightXML = "<dc:complexList>"
                + "<complexListItem><listItem><item>jack</item></listItem></complexListItem>"
                + "</dc:complexList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff();
        expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedListPropDiff = new ListPropertyDiff();
        expectedListPropDiff.addDiff(new SimplePropertyDiff("joe", "jack"));
        expectedListPropDiff.addDiff(new SimplePropertyDiff("john", null));
        expectedComplexPropDiff.putDiff("listItem", expectedListPropDiff);
        expectedFieldDiff.addDiff(expectedComplexPropDiff);
    }

    /**
     * Tests a TEXT_VALUE diff in a complex property.
     * 
     * @throws ClientException the client exception
     */
    @Test
    public void testTextValueComplexPropertyDiff() throws ClientException {

        // Simple complex type
        String leftXML = "<dc:complex><stringItem>joe</stringItem><booleanItem>true</booleanItem></dc:complex>";
        String rightXML = "<dc:complex><stringItem>jack</stringItem><booleanItem>true</booleanItem></dc:complex>";

        PropertyDiff propertyDiff = getPropertyDiff(leftXML, rightXML, 1,
                "complex");

        ComplexPropertyDiff expectedFieldDiff = new ComplexPropertyDiff();
        expectedFieldDiff.putDiff("stringItem", new SimplePropertyDiff("joe",
                "jack"));
        checkComplexFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex type with a nested list item with missing node on the right
        // side
        leftXML = "<dc:complex>"
                + "<listItem><item>joe</item><item>jack</item></listItem>"
                + "<booleanItem>true</booleanItem></dc:complex>";
        rightXML = "<dc:complex>" + "<listItem><item>john</item></listItem>"
                + "<booleanItem>false</booleanItem></dc:complex>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complex");

        expectedFieldDiff = new ComplexPropertyDiff();
        ListPropertyDiff expectedListPropDiff = new ListPropertyDiff();
        expectedListPropDiff.addDiff(new SimplePropertyDiff("joe", "john"));
        expectedListPropDiff.addDiff(new SimplePropertyDiff("jack", null));
        expectedFieldDiff.putDiff("listItem", expectedListPropDiff);
        expectedFieldDiff.putDiff("booleanItem", new SimplePropertyDiff("true",
                "false"));

        checkComplexFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex type with a nested complex item
        leftXML = "<dc:complex>"
                + "<complexItem><stringItem>joe</stringItem><integerItem>10</integerItem></complexItem>"
                + "<booleanItem>true</booleanItem></dc:complex>";
        rightXML = "<dc:complex>"
                + "<complexItem><stringItem>jack</stringItem><integerItem>20</integerItem></complexItem>"
                + "<booleanItem>false</booleanItem></dc:complex>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complex");

        expectedFieldDiff = new ComplexPropertyDiff();
        ComplexPropertyDiff expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("stringItem", new SimplePropertyDiff(
                "joe", "jack"));
        expectedComplexPropDiff.putDiff("integerItem", new SimplePropertyDiff(
                "10", "20"));
        expectedFieldDiff.putDiff("complexItem", expectedComplexPropDiff);
        expectedFieldDiff.putDiff("booleanItem", new SimplePropertyDiff("true",
                "false"));

        checkComplexFieldDiff(propertyDiff, expectedFieldDiff);
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
        String leftXML = "<dc:contributors><item>joe</item></dc:contributors>";
        String rightXML = "<dc:contributors><item>joe</item><item>jack</item></dc:contributors>";

        PropertyDiff propertyDiff = getPropertyDiff(leftXML, rightXML, 1,
                "contributors");

        ListPropertyDiff expectedFieldDiff = new ListPropertyDiff();
        expectedFieldDiff.addDiff(new SimplePropertyDiff(null, "jack"));
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list
        leftXML = "<dc:complexList>"
                + "<complexItem><firstname>Antoine</firstname><lastname>Taillefer</lastname></complexItem>"
                + "</dc:complexList>";
        rightXML = "<dc:complexList>"
                + "<complexItem><firstname>Antoine</firstname><lastname>Taillefer</lastname></complexItem>"
                + "<complexItem><firstname>John</firstname><lastname>Doe</lastname></complexItem>"
                + "<complexItem><firstname>Jack</firstname><lastname>Nicholson</lastname></complexItem>"
                + "</dc:complexList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff();
        ComplexPropertyDiff expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("firstname", new SimplePropertyDiff(
                null, "John"));
        expectedComplexPropDiff.putDiff("lastname", new SimplePropertyDiff(
                null, "Doe"));
        ComplexPropertyDiff expectedComplexPropDiff2 = new ComplexPropertyDiff();
        expectedComplexPropDiff2.putDiff("firstname", new SimplePropertyDiff(
                null, "Jack"));
        expectedComplexPropDiff2.putDiff("lastname", new SimplePropertyDiff(
                null, "Nicholson"));
        expectedFieldDiff.addDiff(expectedComplexPropDiff);
        expectedFieldDiff.addDiff(expectedComplexPropDiff2);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // List of list
        leftXML = "<dc:listOfList>"
                + "<item><listItem><subListItem>Monday</subListItem><subListItem>Tuesday</subListItem></listItem></item>"
                + "</dc:listOfList>";
        rightXML = "<dc:listOfList>"
                + "<item><listItem><subListItem>Monday</subListItem><subListItem>Tuesday</subListItem></listItem></item>"
                + "<item><listItem><subListItem>Wednesday</subListItem><subListItem>Thursday</subListItem></listItem></item>"
                + "</dc:listOfList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "listOfList");

        expectedFieldDiff = new ListPropertyDiff();
        ListPropertyDiff expectedListPropDiff = new ListPropertyDiff();
        expectedListPropDiff.addDiff(new SimplePropertyDiff(null, "Wednesday"));
        expectedListPropDiff.addDiff(new SimplePropertyDiff(null, "Thursday"));
        expectedFieldDiff.addDiff(expectedListPropDiff);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // // List of list (nested child not found)
        leftXML = "<dc:listOfList>"
                + "<item><listItem><subListItem>Monday</subListItem><subListItem>Tuesday</subListItem></listItem></item>"
                + "</dc:listOfList>";
        rightXML = "<dc:listOfList>"
                + "<item><listItem><subListItem>Monday</subListItem></listItem></item>"
                + "</dc:listOfList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "listOfList");

        expectedFieldDiff = new ListPropertyDiff();
        expectedListPropDiff = new ListPropertyDiff();
        expectedListPropDiff.addDiff(new SimplePropertyDiff("Tuesday", null));
        expectedFieldDiff.addDiff(expectedListPropDiff);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);
    }

    /**
     * Tests CHILD_NODE_NOT_FOUND diff in a complex type property.
     * 
     * @throws ClientException the client exception
     */
    @Test
    public void testChildNodeNotFoundComplexPropertyDiff()
            throws ClientException {

        // Complex type => should never happen
        String leftXML = "<dc:complexType><stringItem>joe</stringItem><booleanItem>true</booleanItem></dc:complexType>";
        String rightXML = "<dc:complexType><stringItem>joe</stringItem></dc:complexType>";

        try {
            getPropertyDiff(leftXML, rightXML, 1, "complexType");
            fail("A CHILD_NODE_NOT_FOUND difference should never be found within a complex type.");
        } catch (ClientException ce) {
            LOGGER.info("Exception catched as expected: " + ce.getMessage());
        }

    }

    /**
     * Tests HAS_CHILD_NODES diff in a simple property.
     * 
     * @throws ClientException the client exception
     */
    @Test
    public void testHasChildNodesSimplePropertyDiff() throws ClientException {

        String leftXML = "<dc:title>joe</dc:title>";
        String rightXML = "<dc:title/>";

        PropertyDiff propertyDiff = getPropertyDiff(leftXML, rightXML, 1,
                "title");

        checkSimpleFieldDiff(propertyDiff, "joe", null);
    }

    /**
     * Tests HAS_CHILD_NODES diff in a list property.
     * 
     * @throws ClientException the client exception
     */
    @Test
    public void testHasChildNodesListPropertyDiff() throws ClientException {

        // Simple list (no child nodes on the right side)
        String leftXML = "<dc:contributors><item>joe</item><item>jack</item></dc:contributors>";
        String rightXML = "<dc:contributors/>";

        PropertyDiff propertyDiff = getPropertyDiff(leftXML, rightXML, 1,
                "contributors");

        ListPropertyDiff expectedFieldDiff = new ListPropertyDiff();
        expectedFieldDiff.addDiff(new SimplePropertyDiff("joe", null));
        expectedFieldDiff.addDiff(new SimplePropertyDiff("jack", null));
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Simple list (no child nodes on the left side)
        leftXML = "<dc:contributors/>";
        rightXML = "<dc:contributors><item>joe</item><item>jack</item></dc:contributors>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "contributors");

        expectedFieldDiff = new ListPropertyDiff();
        expectedFieldDiff.addDiff(new SimplePropertyDiff(null, "joe"));
        expectedFieldDiff.addDiff(new SimplePropertyDiff(null, "jack"));
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // List (item with no child nodes on the right side) => should never
        // happen
        leftXML = "<dc:contributors><item>joe</item><item>jack</item></dc:contributors>";
        rightXML = "<dc:contributors><item>joe</item><item/></dc:contributors>";

        try {
            propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "contributors");
            fail("A HAS_CHILD_NODES difference should never be found on a list item.");
        } catch (ClientException ce) {
            LOGGER.info("Exception catched as expected: " + ce.getMessage());
        }

        // List (item with no child nodes on the left side) => should never
        // happen
        leftXML = "<dc:contributors><item>joe</item><item/></dc:contributors>";
        rightXML = "<dc:contributors><item>joe</item><item>jack</item></dc:contributors>";

        try {
            propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "contributors");
            fail("A HAS_CHILD_NODES difference should never be found on a list item.");
        } catch (ClientException ce) {
            LOGGER.info("Exception catched as expected: " + ce.getMessage());
        }

        // Complex list (empty list on the right side)
        leftXML = "<dc:complexList>"
                + "<complexItem><stringItem>joe</stringItem><integerItem>10</integerItem></complexItem>"
                + "</dc:complexList>";
        rightXML = "<dc:complexList/>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff();
        ComplexPropertyDiff expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("stringItem", new SimplePropertyDiff(
                "joe", null));
        expectedComplexPropDiff.putDiff("integerItem", new SimplePropertyDiff(
                "10", null));
        expectedFieldDiff.addDiff(expectedComplexPropDiff);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list (empty list on the left side)
        leftXML = "<dc:complexList/>";
        rightXML = "<dc:complexList>"
                + "<complexItem><stringItem>joe</stringItem><integerItem>10</integerItem></complexItem>"
                + "</dc:complexList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff();
        expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("stringItem", new SimplePropertyDiff(
                null, "joe"));
        expectedComplexPropDiff.putDiff("integerItem", new SimplePropertyDiff(
                null, "10"));
        expectedFieldDiff.addDiff(expectedComplexPropDiff);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list (complex item with no child nodes on the right side)
        leftXML = "<dc:complexList>"
                + "<complexItem><stringItem>joe</stringItem><integerItem>10</integerItem></complexItem>"
                + "</dc:complexList>";
        rightXML = "<dc:complexList>"
                + "<complexItem><stringItem>jack</stringItem><integerItem/></complexItem>"
                + "</dc:complexList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff();
        expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("stringItem", new SimplePropertyDiff(
                "joe", "jack"));
        expectedComplexPropDiff.putDiff("integerItem", new SimplePropertyDiff(
                "10", null));
        expectedFieldDiff.addDiff(expectedComplexPropDiff);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list (complex item with no child nodes on the left side)
        leftXML = "<dc:complexList>"
                + "<complexItem><stringItem>joe</stringItem><integerItem/></complexItem>"
                + "</dc:complexList>";
        rightXML = "<dc:complexList>"
                + "<complexItem><stringItem>jack</stringItem><integerItem>10</integerItem></complexItem>"
                + "</dc:complexList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff();
        expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("stringItem", new SimplePropertyDiff(
                "joe", "jack"));
        expectedComplexPropDiff.putDiff("integerItem", new SimplePropertyDiff(
                null, "10"));
        expectedFieldDiff.addDiff(expectedComplexPropDiff);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list (complex item with no child nodes on the left side and
        // empty list on the right side)
        leftXML = "<dc:complexList>"
                + "<complexItem><stringItem>joe</stringItem><integerItem/></complexItem>"
                + "</dc:complexList>";
        rightXML = "<dc:complexList/>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff();
        expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("stringItem", new SimplePropertyDiff(
                "joe", null));
        expectedComplexPropDiff.putDiff("integerItem", new SimplePropertyDiff(
                "", null));
        expectedFieldDiff.addDiff(expectedComplexPropDiff);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex list (complex item with no child nodes on the right side and
        // empty list on the left side)
        leftXML = "<dc:complexList/>";
        rightXML = "<dc:complexList>"
                + "<complexItem><stringItem>joe</stringItem><integerItem/></complexItem>"
                + "</dc:complexList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexList");

        expectedFieldDiff = new ListPropertyDiff();
        expectedComplexPropDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff.putDiff("stringItem", new SimplePropertyDiff(
                null, "joe"));
        expectedComplexPropDiff.putDiff("integerItem", new SimplePropertyDiff(
                null, ""));
        expectedFieldDiff.addDiff(expectedComplexPropDiff);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // List of list (empty on the right side)
        leftXML = "<dc:listOfList>"
                + "<item><listItem><subListItem>Monday</subListItem><subListItem>Tuesday</subListItem></listItem></item>"
                + "</dc:listOfList>";
        rightXML = "<dc:complexList/>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "listOfList");

        expectedFieldDiff = new ListPropertyDiff();
        ListPropertyDiff expectedListPropDiff = new ListPropertyDiff();
        expectedListPropDiff.addDiff(new SimplePropertyDiff("Monday", null));
        expectedListPropDiff.addDiff(new SimplePropertyDiff("Tuesday", null));
        expectedFieldDiff.addDiff(expectedListPropDiff);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // List of list (empty on the left side)
        leftXML = "<dc:listOfList/>";
        rightXML = "<dc:listOfList>"
                + "<item><listItem><subListItem>Monday</subListItem><subListItem>Tuesday</subListItem></listItem></item>"
                + "</dc:listOfList>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "listOfList");

        expectedFieldDiff = new ListPropertyDiff();
        expectedListPropDiff = new ListPropertyDiff();
        expectedListPropDiff.addDiff(new SimplePropertyDiff(null, "Monday"));
        expectedListPropDiff.addDiff(new SimplePropertyDiff(null, "Tuesday"));
        expectedFieldDiff.addDiff(expectedListPropDiff);
        checkListFieldDiff(propertyDiff, expectedFieldDiff);

        // List of list (nested child node with no child nodes on the right
        // side) => should never happen
        leftXML = "<dc:listOfList>"
                + "<item><listItem><subListItem>Monday</subListItem><subListItem>Tuesday</subListItem></listItem></item>"
                + "</dc:listOfList>";
        rightXML = "<dc:listOfList>"
                + "<item><listItem><subListItem>Wednesday</subListItem><subListItem/></listItem></item>"
                + "</dc:listOfList>";

        try {
            propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "listOfList");
            fail("A HAS_CHILD_NODES difference should never be found on a list item.");
        } catch (ClientException ce) {
            LOGGER.info("Exception catched as expected: " + ce.getMessage());
        }

        // List of list (nested child node with no child nodes on the left side)
        // => should never happen
        leftXML = "<dc:listOfList>"
                + "<item><listItem><subListItem>Monday</subListItem><subListItem/></listItem></item>"
                + "</dc:listOfList>";
        rightXML = "<dc:listOfList>"
                + "<item><listItem><subListItem>Wednesday</subListItem><subListItem>Tuesday</subListItem></listItem></item>"
                + "</dc:listOfList>";

        try {
            propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "listOfList");
            fail("A HAS_CHILD_NODES difference should never be found on a list item.");
        } catch (ClientException ce) {
            LOGGER.info("Exception catched as expected: " + ce.getMessage());
        }

    }

    /**
     * Tests HAS_CHILD_NODES diff in a complex property.
     * 
     * @throws ClientException the client exception
     */
    @Test
    public void testHasChildNodesComplexPropertyDiff() throws ClientException {

        // Simple complex type => should never happen
        String leftXML = "<dc:complexType><stringItem>joe</stringItem><booleanItem>true</booleanItem></dc:complexType>";
        String rightXML = "<dc:complexType/>";

        try {
            getPropertyDiff(leftXML, rightXML, 1, "complexType");
            fail("A HAS_CHILD_NODES difference should never be found on a complex type.");
        } catch (ClientException ce) {
            LOGGER.info("Exception catched as expected: " + ce.getMessage());
        }

        // Simple complex type (item with no child nodes)
        leftXML = "<dc:complexType><stringItem>joe</stringItem><booleanItem>true</booleanItem></dc:complexType>";
        rightXML = "<dc:complexType><stringItem>joe</stringItem><booleanItem/></dc:complexType>";

        PropertyDiff propertyDiff = getPropertyDiff(leftXML, rightXML, 1,
                "complexType");

        ComplexPropertyDiff expectedFieldDiff = new ComplexPropertyDiff();
        expectedFieldDiff.putDiff("booleanItem", new SimplePropertyDiff("true",
                null));
        checkComplexFieldDiff(propertyDiff, expectedFieldDiff);

        // Complex type with a nested list item
        leftXML = "<dc:complexType>"
                + "<stringItem>joe</stringItem>"
                + "<listItem><item>Monday</item><item>Tuesday</item></listItem>"
                + "</dc:complexType>";
        rightXML = "<dc:complexType>"
                + "<stringItem>jack</stringItem><listItem/></dc:complexType>";

        propertyDiff = getPropertyDiff(leftXML, rightXML, 1, "complexType");

        expectedFieldDiff = new ComplexPropertyDiff();
        ListPropertyDiff expectedListPropertyDiff = new ListPropertyDiff();
        expectedListPropertyDiff.addDiff(new SimplePropertyDiff("Monday", null));
        expectedListPropertyDiff.addDiff(new SimplePropertyDiff("Tuesday", null));
        expectedFieldDiff.putDiff("listItem", expectedListPropertyDiff);
        expectedFieldDiff.putDiff("stringItem", new SimplePropertyDiff("joe",
                "jack"));
        checkComplexFieldDiff(propertyDiff, expectedFieldDiff);
    }

    /**
     * Wraps xml string in a schema element.
     * 
     * @param xml the xml
     * @return the string
     */
    protected final String wrapXML(String xml) {

        StringBuilder sb = new StringBuilder(
                "<schema xmlns:dc=\"dcNS\" name= \"dublincore\">");
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
    protected final PropertyDiff getPropertyDiff(String leftXML,
            String rightXML, int diffCount, String field)
            throws ClientException {

        DocumentDiff docDiff = docDiffService.diff(wrapXML(leftXML),
                wrapXML(rightXML));
        SchemaDiff schemaDiff = checkSchemaDiff(docDiff, "dublincore",
                diffCount);

        return schemaDiff.getFieldDiff(field);
    }

}
