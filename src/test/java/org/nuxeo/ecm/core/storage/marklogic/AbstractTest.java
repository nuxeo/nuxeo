/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
<<<<<<< HEAD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
=======
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
>>>>>>> a4c6e32... NXP-19244: Implements basic NXQL in MarkLogic
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
<<<<<<< HEAD
 *
=======
 *
>>>>>>> a4c6e32... NXP-19244: Implements basic NXQL in MarkLogic
 * Contributors:
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.dom4j.Attribute;
import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;
import org.dom4j.Text;
import org.dom4j.util.NodeComparator;
import org.junit.BeforeClass;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.nuxeo.ecm.core.storage.State;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.impl.DatabaseClientImpl;

public abstract class AbstractTest {

    @SuppressWarnings("unchecked")
    private static final Comparator<Node> NODE_COMPARATOR = new NodeComparator();

    protected static DatabaseClient CLIENT;

    @BeforeClass
    public static void beforeClass() {
        CLIENT = Mockito.mock(DatabaseClientImpl.class, Answers.CALLS_REAL_METHODS.get());
    }

    public String readFile(String file) throws Exception {
        return new String(Files.readAllBytes(Paths.get(this.getClass().getResource("/" + file).toURI())));
    }

    public void assertXMLFileAgainstString(String file, String actual) throws Exception {
        assertXMLEquals(readFile(file), actual);
    }

    public void assertXMLEquals(String expected, String actual) throws DocumentException, IOException {
        assertXMLEquals(DocumentHelper.parseText(expected), DocumentHelper.parseText(actual));
    }

    public void assertXMLEquals(Document expected, Document actual) {
        expected.normalize();
        actual.normalize();

        assertBranchEquals(expected, actual);
    }

    public void assertXMLEquals(Attribute expected, Attribute actual) {
        assertXMLEquals(expected.getQName(), actual.getQName());
        assertEquals(format("Element %s and %s don't have same attribute value.", expected, actual),
                expected.getValue(), actual.getValue());
    }

    public void assertXMLEquals(Element expected, Element actual) {
        assertXMLEquals(expected.getQName(), actual.getQName());

        int expectedAttributeCount = expected.attributeCount();
        int actualAttributeCount = actual.attributeCount();
        assertEquals(format("Element %s and %s don't have same attribute count.", expected, actual),
                expectedAttributeCount, actualAttributeCount);

        for (int i = 0; i < expectedAttributeCount; i++) {
            assertXMLEquals(expected.attribute(i), actual.attribute(i));
        }
        assertBranchEquals(expected, actual);
    }

    public void assertXMLEquals(Text expected, Text actual) {
        assertEquals(format("Text nodes %s and %s don't have same name.", expected, actual), expected.getName(),
                actual.getName());
        assertEquals(format("Text nodes %s and %s don't have same text.", expected, actual), expected.getText(),
                actual.getText());
    }

    public void assertXMLEquals(Namespace expected, Namespace actual) {
        if (expected != null && actual != null) {
            assertEquals(format("Namespaces %s and %s don't have same prefix.", expected, actual),
                    expected.getPrefix(), actual.getPrefix());
            assertEquals(format("Namespaces %s and %s don't have same uri.", expected, actual), expected.getURI(),
                    actual.getURI());
        } else if (expected == null ^ actual == null) {
            fail(format("Namespaces %s and %s can't be equals", expected, actual));
        }
    }

    public void assertBranchEquals(Branch expected, Branch actual) {
        Iterable<Node> expectedNodesIterable = expected::nodeIterator;
        Iterable<Node> actualNodesIterable = actual::nodeIterator;
        List<Node> expectedNodes = StreamSupport.stream(expectedNodesIterable.spliterator(), false)
                                                .filter(this::isNotEmptyOrNewLine)
                                                .sorted(NODE_COMPARATOR)
                                                .collect(Collectors.toList());
        List<Node> actualNodes = StreamSupport.stream(actualNodesIterable.spliterator(), false)
                                              .filter(this::isNotEmptyOrNewLine)
                                              .sorted(NODE_COMPARATOR)
                                              .collect(Collectors.toList());

        int expectedNodeCount = expectedNodes.size();
        int actualNodeCount = actualNodes.size();
        assertEquals(format("Nodes %s and %s don't have same node count.", expected, actual), expectedNodeCount,
                actualNodeCount);

        for (int i = 0; i < expectedNodeCount; i++) {
            assertXMLEquals(expectedNodes.get(i), actualNodes.get(i));
        }
    }

    public void assertXMLEquals(Node expected, Node actual) {
        short expectedNodeType = expected.getNodeType();
        short actualNodeType = actual.getNodeType();
        assertEquals(format("Nodes %s and %s are not of same type.", expected, actual), expectedNodeType,
                actualNodeType);

        switch (expectedNodeType) {
        case Node.DOCUMENT_NODE:
            assertXMLEquals((Document) expected, (Document) actual);
            break;
        case Node.ATTRIBUTE_NODE:
            assertXMLEquals((Attribute) expected, (Attribute) actual);
            break;
        case Node.ELEMENT_NODE:
            assertXMLEquals((Element) expected, (Element) actual);
            break;
        case Node.TEXT_NODE:
            assertXMLEquals((Text) expected, (Text) actual);
            break;
        case Node.NAMESPACE_NODE:
            assertXMLEquals((Namespace) expected, (Namespace) actual);
            break;
        default:
            fail(String.format("Node type '%s' is not valid.", expectedNodeType));
        }
    }

    public void assertXMLEquals(QName expected, QName actual) {
        assertXMLEquals(expected.getNamespace(), actual.getNamespace());
        assertEquals(String.format("QNames %s and %s don't have same name.", expected, actual), expected.getName(),
                actual.getName());
    }

    public String format(String format, Node... args) {
        if (args == null) {
            return format;
        }
        return String.format(format, Arrays.stream(args).map(Node::asXML).toArray());
    }

    public boolean isNotEmptyOrNewLine(Node node) {
        return node.getNodeType() != Node.TEXT_NODE
                || (!"".equals(node.getText()) && !node.getText().matches("\\s*\n\\s*"));
    }

}
