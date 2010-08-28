/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.jbpm.core.deployer;

import java.net.URL;
import java.security.NoSuchAlgorithmException;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class MD5HasherTest extends TestCase {

    private static final String xmlFolder = "xml/";
    private static final String smallXmlFile = xmlFolder + "smallXmlFile.xml";
    private static final String smallCommentedFile = xmlFolder
            + "smallCommentedFile.xml";

    private MD5Hasher hasher;

    @Override
    protected void setUp() throws Exception {
        hasher = new MD5Hasher();
    }

    public void testGetDomDocument() throws Exception {
        URL url = getURL(smallXmlFile);
        Document document = hasher.getDomDocument(url);
        assertNotNull(document);
        assertEquals("foo", document.getFirstChild().getNodeName());
    }

    private static URL getURL(String path) {
        return Thread.currentThread().getContextClassLoader().getResource(path);
    }

    public void testTrimDocument() throws Exception {
        Document document = hasher.getDomDocument(getURL(smallCommentedFile));
        NodeList list = document.getChildNodes();
        assertEquals("foo", list.item(0).getNodeName());

        list = list.item(0).getChildNodes();
        assertNode(list, "#text", 0);
        assertNode(list, "#comment", 1);
        assertNode(list, "#text", 2);
        assertNode(list, "bar", 3);
        assertNode(list, "#text", 4);
        assertNode(list, "#comment", 5);
        assertNode(list, "#text", 6);

        document = hasher.trimDocument(document);
        list = document.getChildNodes();
        assertNode(list, "foo", 0);

        list = list.item(0).getChildNodes();
        assertEquals(1, list.getLength());
        assertNode(list, "bar", 0);
    }

    public void testGetBytes() throws Exception {
        URL url = getURL(smallXmlFile);
        Document document = hasher.getDomDocument(url);
        byte[] result = hasher.getBytes(document);
        assertNotNull(result);
    }

    public void testMD5() throws NoSuchAlgorithmException {
        // taken from: http://en.wikipedia.org/wiki/MD5
        assertEquals("d41d8cd98f00b204e9800998ecf8427e",
                hasher.MD5("".getBytes()));
        assertEquals(
                "e4d909c290d0fb1ca068ffaddf22cbd0",
                hasher.MD5("The quick brown fox jumps over the lazy dog.".getBytes()));
        assertEquals(
                "9e107d9d372bb6826bd81d3542a419d6",
                hasher.MD5("The quick brown fox jumps over the lazy dog".getBytes()));
    }

    public void testGetMD5FromURL() throws Exception {
        assertEquals(hasher.getMD5FromURL(getURL(smallCommentedFile)),
                hasher.getMD5FromURL(getURL(smallXmlFile)));
    }

    private static void assertNode(NodeList list, String nodeName, int index) {
        assertEquals(nodeName, list.item(index).getNodeName());
    }

}
