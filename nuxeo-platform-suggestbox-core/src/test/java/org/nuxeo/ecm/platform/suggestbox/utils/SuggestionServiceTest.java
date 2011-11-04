/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.suggestbox.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionService;
import org.nuxeo.runtime.api.Framework;

public class SuggestionServiceTest extends SQLRepositoryTestCase {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(SuggestionServiceTest.class);

    protected SuggestionService sugService;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // myself
        deployBundle("org.nuxeo.ecm.platform.suggestbox.core");

        // create document documents
        openSession();
        DocumentModel domain = session.createDocumentModel("/",
                "default-domain", "Folder");
        session.createDocument(domain);
        session.save();

        //
        sugService = Framework.getService(SuggestionService.class);
        assertNotNull(sugService);
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

}
