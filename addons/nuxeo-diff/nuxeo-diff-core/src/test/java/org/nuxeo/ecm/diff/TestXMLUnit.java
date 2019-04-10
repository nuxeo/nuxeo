/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     ataillefer
 */
package org.nuxeo.ecm.diff;

import java.util.List;

import org.custommonkey.xmlunit.AbstractNodeTester;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.ElementNameAndTextQualifier;
import org.custommonkey.xmlunit.IgnoreTextAndAttributeValuesDifferenceListener;
import org.custommonkey.xmlunit.NodeTest;
import org.custommonkey.xmlunit.NodeTestException;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.examples.CountingNodeTester;
import org.dom4j.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * Tests and illustrates {@link XMLUnit} basic features.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class TestXMLUnit extends XMLTestCase {

    /**
     * Test for equality.
     *
     * @throws Exception the exception
     */
    public void testForEquality() throws Exception {
        String myControlXML = "<msg><uuid>0x00435A8C</uuid></msg>";
        String myTestXML = "<msg><localId>2376</localId></msg>";
        assertXMLNotEqual("Comparing test xml to control xml", myControlXML, myTestXML);
    }

    /**
     * Test xml identical.
     *
     * @throws Exception the exception
     */
    public void testXMLIdentical() throws Exception {
        String myControlXML = "<struct><int>3</int><boolean>false</boolean></struct>";
        String myTestXML = "<struct><boolean>false</boolean><int>3</int></struct>";
        Diff myDiff = new Diff(myControlXML, myTestXML);
        assertTrue("XML similar " + myDiff.toString(), myDiff.similar());
        assertFalse("XML identical " + myDiff.toString(), myDiff.identical());
    }

    /**
     * Test all differences.
     *
     * @throws Exception the exception
     */
    @SuppressWarnings("unchecked")
    public void testAllDifferences() throws Exception {
        String myControlXML = "<news><item id=\"1\">War</item>" + "<item id=\"2\">Plague</item>"
                + "<item id=\"3\">Famine</item></news>";
        String myTestXML = "<news><item id=\"1\">Peace</item>" + "<item id=\"2\">Health</item>"
                + "<item id=\"3\">Plenty</item></news>";
        DetailedDiff myDiff = new DetailedDiff(new Diff(myControlXML, myTestXML));
        List<Difference> allDifferences = myDiff.getAllDifferences();
        assertEquals(myDiff.toString(), 3, allDifferences.size());

    }

    /**
     * Test compare to skeleton xml.
     *
     * @throws Exception the exception
     */
    public void testCompareToSkeletonXML() throws Exception {

        String myControlXML = "<location><street-address>22 any street</street-address><postcode>XY00 99Z</postcode></location>";
        String myTestXML = "<location><street-address>20 east cheap</street-address><postcode>EC3M 1EB</postcode></location>";

        Diff myDiff = new Diff(myControlXML, myTestXML);
        assertFalse("test XML matches control skeleton XML", myDiff.similar());

        myDiff = new Diff(myControlXML, myTestXML);
        DifferenceListener myDifferenceListener = new IgnoreTextAndAttributeValuesDifferenceListener();
        myDiff.overrideDifferenceListener(myDifferenceListener);
        assertTrue("test XML matches control skeleton XML", myDiff.similar());
    }

    /**
     * Test repeated child elements.
     *
     * @throws Exception the exception
     */
    public void testRepeatedChildElements() throws Exception {
        String myControlXML = "<suite>" + "<test status=\"pass\">FirstTestCase</test>"
                + "<test status=\"pass\">SecondTestCase</test></suite>";
        String myTestXML = "<suite>" + "<test status=\"pass\">SecondTestCase</test>"
                + "<test status=\"pass\">FirstTestCase</test></suite>";
        assertXMLNotEqual("Repeated child elements in different sequence order are not equal by default", myControlXML,
                myTestXML);
        Diff myDiff = new Diff(myControlXML, myTestXML);
        myDiff.overrideElementQualifier(new ElementNameAndTextQualifier());
        assertXMLEqual(
                "But they are equal when an ElementQualifier controls which test element is compared with each control element",
                myDiff, true);
    }

    /**
     * Test x paths.
     *
     * @throws Exception the exception
     */
    public void testXPaths() throws Exception {
        String mySolarSystemXML = "<solar-system>" + "<planet name='Earth' position='3' supportsLife='yes'/>"
                + "<planet name='Venus' position='4'/></solar-system>";
        assertXpathExists("//planet[@name='Earth']", mySolarSystemXML);
        assertXpathNotExists("//star[@name='alpha centauri']", mySolarSystemXML);
        assertXpathsEqual("//planet[@name='Earth']", "//planet[@position='3']", mySolarSystemXML);
        assertXpathsNotEqual("//planet[@name='Venus']", "//planet[@supportsLife='yes']", mySolarSystemXML);
    }

    // we
    /**
     * Test x path values.
     *
     * @throws Exception the exception
     */
    public void testXPathValues() throws Exception {
        String myJavaFlavours = "<java-flavours>" + "<jvm current='some platforms'>1.1.x</jvm>"
                + "<jvm current='no'>1.2.x</jvm>" + "<jvm current='yes'>1.3.x</jvm>"
                + "<jvm current='yes' latest='yes'>1.4.x</jvm></java-flavours>";
        assertXpathEvaluatesTo("2", "count(//jvm[@current='yes'])", myJavaFlavours);
        assertXpathValuesEqual("//jvm[4]/@latest", "//jvm[4]/@current", myJavaFlavours);
        assertXpathValuesNotEqual("//jvm[2]/@current", "//jvm[3]/@current", myJavaFlavours);
    }

    /**
     * Test counting node tester.
     *
     * @throws Exception the exception
     */
    public void testCountingNodeTester() throws Exception {
        String testXML = "<fibonacci><val>1</val><val>2</val><val>3</val>" + "<val>5</val><val>9</val></fibonacci>";
        CountingNodeTester countingNodeTester = new CountingNodeTester(5);
        assertNodeTestPasses(testXML, countingNodeTester, Node.TEXT_NODE);
    }

    /**
     * Test custom node tester.
     *
     * @throws Exception the exception
     */
    public void testCustomNodeTester() throws Exception {
        String testXML = "<fibonacci><val>1</val><val>2</val><val>3</val>" + "<val>5</val><val>8</val></fibonacci>";
        NodeTest nodeTest = new NodeTest(testXML);
        assertNodeTestPasses(nodeTest, new FibonacciNodeTester(), new short[] { Node.TEXT_NODE, Node.ELEMENT_NODE },
                true);
    }

    /**
     * FibonacciNodeTester.
     */
    private class FibonacciNodeTester extends AbstractNodeTester {

        private int nextVal = 1;

        private int lastVal = 1;

        public void testText(Text text) throws NodeTestException {
            int val = Integer.parseInt(text.getData());
            if (nextVal != val) {
                throw new NodeTestException("Incorrect value", text);
            }
            nextVal = val + lastVal;
            lastVal = val;
        }

        public void testElement(Element element) throws NodeTestException {
            String name = element.getLocalName();
            if ("fibonacci".equals(name) || "val".equals(name)) {
                return;
            }
            throw new NodeTestException("Unexpected element", element);
        }

        public void noMoreNodes(NodeTest nodeTest) throws NodeTestException {
        }

    }

    /**
     * Test unmatched nodes comparison.
     *
     * @throws Exception the exception
     */
    @SuppressWarnings("unchecked")
    public void testCompareUnmatchedNodes() throws Exception {
        String myControlXML = "<document><item>First item</item>" + "<item>Second item</item></document>";
        String myTestXML = "<document><item>First item</item></document>";
        assertXMLNotEqual("Test XML has a missing child node", myControlXML, myTestXML);

        // ---------------------------
        // Compare unmatched nodes
        // ---------------------------
        // First make sure that we have the default behavior for comparing
        // unmatched nodes by doing XMLUnit.setCompareUnmatched(true).
        // Indeed, it may have been set to false by a previous test.
        XMLUnit.setCompareUnmatched(true);
        DetailedDiff myDiff = new DetailedDiff(new Diff(myControlXML, myTestXML));
        List<Difference> allDifferences = myDiff.getAllDifferences();
        assertEquals("Wrong number of differences", 3, allDifferences.size());

        Difference diff1 = allDifferences.get(0);
        assertEquals("Wrong difference type", DifferenceConstants.CHILD_NODELIST_LENGTH_ID, diff1.getId());

        // "CHILD_NODE_NOT_FOUND on the test side" strange behavior
        // => considered as a TEXT_VALUE difference
        Difference diff2 = allDifferences.get(1);
        assertEquals("Wrong difference type", DifferenceConstants.TEXT_VALUE_ID, diff2.getId());

        Difference diff3 = allDifferences.get(2);
        assertEquals("Wrong difference type", DifferenceConstants.CHILD_NODELIST_SEQUENCE_ID, diff3.getId());

        // ---------------------------
        // Don't compare unmatched nodes
        // ---------------------------
        XMLUnit.setCompareUnmatched(false);
        myDiff = new DetailedDiff(new Diff(myControlXML, myTestXML));
        allDifferences = myDiff.getAllDifferences();
        assertEquals("Wrong number of differences", 2, allDifferences.size());

        diff1 = allDifferences.get(0);
        assertEquals("Wrong difference type", DifferenceConstants.CHILD_NODELIST_LENGTH_ID, diff1.getId());

        diff2 = allDifferences.get(1);
        assertEquals("Wrong difference type", DifferenceConstants.CHILD_NODE_NOT_FOUND_ID, diff2.getId());

    }

}
