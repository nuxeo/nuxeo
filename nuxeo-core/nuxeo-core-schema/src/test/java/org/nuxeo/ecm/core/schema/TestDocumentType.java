/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.schema;

import junit.framework.TestCase;

public class TestDocumentType extends TestCase {

    public void testTrivialDocumentType() {
        DocumentType docType = new DocumentTypeImpl((DocumentType) null, "doc type");

        assertEquals("doc type", docType.getName());

        assertTrue(docType.isFile());
        assertFalse(docType.isFolder());
        assertFalse(docType.isOrdered());
    }

    public void testFileDocumentType() {
        DocumentType docType = new DocumentTypeImpl((DocumentType) null, "doc type", null,
                null);

        assertTrue(docType.isFile());
        assertFalse(docType.isFolder());
        assertFalse(docType.isOrdered());
    }

    public void testFolderDocumentType() {
        DocumentType docType = new DocumentTypeImpl((DocumentType) null, "doc type", null,
                new String[]{"Folderish"});

        assertFalse(docType.isFile());
        assertTrue(docType.isFolder());
        assertFalse(docType.isOrdered());
    }

    public void testOrderedFolderDocumentType() {
        DocumentType docType = new DocumentTypeImpl((DocumentType) null, "doc type", null,
                new String[]{"Folderish", "Orderable"});

        assertFalse(docType.isFile());
        assertTrue(docType.isFolder());
        assertTrue(docType.isOrdered());
    }

}
