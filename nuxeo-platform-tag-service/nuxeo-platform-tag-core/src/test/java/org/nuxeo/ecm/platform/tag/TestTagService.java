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
 */

package org.nuxeo.ecm.platform.tag;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.tag.persistence.TagPersistenceProvider;
import org.nuxeo.runtime.api.Framework;

public class TestTagService extends RepositoryOSGITestCase {

    protected static final Log log = LogFactory.getLog(TestTagService.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.comment.core");
        deployBundle("org.nuxeo.ecm.platform.tag.service.tests");
        deployContrib("org.nuxeo.ecm.platform.tag.service.tests",
                "OSGI-INF/tag-service-core-types.xml");
        deployContrib("org.nuxeo.ecm.platform.tag.service.tests",
                "OSGI-INF/TagService.xml");
    }

    @Override
    public void tearDown() throws Exception {
        TagPersistenceProvider.getInstance().closePersistenceUnit();
        super.tearDown();
    }

    public TagService getTagService() {
        return Framework.getLocalService(TagService.class);
    }

    public void testServiceTagInitialization() {
        TagService tagService = getTagService();
        assertNotNull("Failed to get tag service.", tagService);
    };

    public void testTagCreation() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.comment.core",
                "OSGI-INF/comment-schemas-contrib.xml");
        TagService tagService = getTagService();
        openRepository();
        DocumentModel tagRoot = tagService.getRootTag(coreSession);
        assertNotNull(tagRoot);
        tagService.getOrCreateTag(tagRoot, "tag1", true);
        DocumentModel tag = null;
        for (DocumentModel tagChild : coreSession.getChildren(tagRoot.getRef())) {
            if (tagChild.getProperty("tag", "label").toString().equals("tag1")) {
                tag = tagChild;
                break;
            }
        }
        assertNotNull(
                "Unable to find created tag. Probably service failing on creation.",
                tag);
        assertTrue("Private flag is not correctly set.",
                ((Boolean) tag.getProperty("tag", "private")).booleanValue());
        assertTrue("", tag.getProperty("dublincore", "creator").equals(
                coreSession.getPrincipal().getName()));

    }

}
