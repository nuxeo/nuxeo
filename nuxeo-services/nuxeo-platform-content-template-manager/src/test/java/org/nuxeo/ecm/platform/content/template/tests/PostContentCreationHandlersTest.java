/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.content.template.tests;

import java.util.Collections;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.content.template.service.ContentTemplateService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class PostContentCreationHandlersTest extends SQLRepositoryTestCase {

    protected ContentTemplateService service;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployContrib("org.nuxeo.ecm.platform.content.template.tests",
                "test-content-template-handlers-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.content.template.tests",
                "test-content-template-handlers-contrib.xml");
        fireFrameworkStarted();
        openSession();

        service = Framework.getLocalService(ContentTemplateService.class);
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    @Test
    public void testHandler() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModelList rootChildren = session.getChildren(root.getRef());
        assertEquals(2, rootChildren.size());

        // query result order is not fixed
        if (!"default-domain".equals(rootChildren.get(0).getName())) {
            Collections.reverse(rootChildren);
        }
        assertEquals("default-domain", rootChildren.get(0).getName());

        DocumentModel child = rootChildren.get(1);
        assertNotNull(child);
        assertEquals(SimplePostContentCreationHandler.DOC_NAME, child.getName());
        assertEquals(SimplePostContentCreationHandler.DOC_TYPE, child.getType());
    }

}
