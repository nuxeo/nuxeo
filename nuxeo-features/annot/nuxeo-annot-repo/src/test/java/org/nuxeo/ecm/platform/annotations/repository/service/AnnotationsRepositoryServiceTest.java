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

package org.nuxeo.ecm.platform.annotations.repository.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author Alexandre Russel
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.relations", //
        "org.nuxeo.ecm.annotations", //
        "org.nuxeo.ecm.annotations.repository", //
        "org.nuxeo.ecm.relations.jena", //
        "org.nuxeo.ecm.platform.url.core", //
})
public class AnnotationsRepositoryServiceTest {

    @Test
    public void testServices() throws Exception {
        AnnotationsRepositoryService service = Framework.getService(AnnotationsRepositoryService.class);
        assertNotNull(service);
        AnnotationsRepositoryServiceImpl impl = (AnnotationsRepositoryServiceImpl) service;
        DocumentAnnotability annotability = impl.getAnnotability();
        assertNotNull(annotability);
        assertTrue(annotability instanceof DefaultDocumentAnnotability);
    }

}
