/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Vincent Dutat
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api;

import org.junit.Test;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.schema.FacetNames;

public class TestDocumentModelIterator extends BaseTestCase {

    @Test
    public void testDocumentModelIteratorWithFilter() throws ClientException {
        DocumentModel doc = session.createDocumentModel("/", "document1",
                "File");
        doc.setPropertyValue("dc:title", "Document1");
        doc = session.createDocument(doc);
        session.save();

        DocumentModelIterator it = session.queryIt("SELECT * FROM Document",
                new FacetFilter(FacetNames.FOLDERISH, false), 100);
        int counted = 0;
        for (; it.hasNext(); it.next()) {
            counted++;
        }
        assertEquals(counted, it.size());
    }

}
