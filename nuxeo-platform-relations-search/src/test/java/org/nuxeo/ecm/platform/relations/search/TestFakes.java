/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.relations.search;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;

/**
 * Test the fakes the tests rely on
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
public class TestFakes extends TestCase {

    public void testDocumentResourceAdapter() {
        ResourceAdapter adapter = new FakeDocumentResourceAdapter();
        adapter.setNamespace(Constants.DOCUMENT_NAMESPACE);
        DocumentModel docModel = (DocumentModel) adapter.getResourceRepresentation(
                new QNameResourceImpl(Constants.DOCUMENT_NAMESPACE,
                        "foobar"));
        assertEquals(Constants.REPOSITORY_NAME,
                docModel.getRepositoryName());
        assertEquals(new IdRef("foobar"), docModel.getRef());
    }


}
