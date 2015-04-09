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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.content.template.service.ContentTemplateService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.content.template")
@LocalDeploy({ "org.nuxeo.ecm.platform.content.template.tests:test-content-template-handlers-contrib.xml",
        "org.nuxeo.ecm.platform.content.template.tests:test-content-template-handlers-contrib.xml" })
public class PostContentCreationHandlersTest {

    @Inject
    protected ContentTemplateService service;

    @Inject
    protected CoreSession session;

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
