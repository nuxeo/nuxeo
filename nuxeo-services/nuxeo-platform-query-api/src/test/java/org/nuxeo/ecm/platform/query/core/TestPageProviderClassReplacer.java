/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.platform.query.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 6.0
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.query.api")
@LocalDeploy({ "org.nuxeo.ecm.platform.query.api.test:test-pageprovider-contrib.xml",
        "org.nuxeo.ecm.platform.query.api.test:test-pageprovider-classreplacer-contrib.xml",
        "org.nuxeo.ecm.platform.query.api.test:test-schemas-contrib.xml", })
public class TestPageProviderClassReplacer {

    @Inject
    protected PageProviderService pps;

    @Test
    public void testReplacer() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppd = pps.getPageProviderDefinition("CURRENT_DOCUMENT_CHILDREN");
        PageProvider<?> pp = pps.getPageProvider("CURRENT_DOCUMENT_CHILDREN", ppd, null, null, 1L, 0L, null);
        assertNotNull(pp);
        assertTrue("wrong class " + pp, pp instanceof CoreQueryAndFetchPageProvider);

        pp = pps.getPageProvider("foo", ppd, null, null, 1L, 0L, null);
        assertNotNull(pp);
        assertTrue("wrong class " + pp, pp instanceof CoreQueryAndFetchPageProvider);

        pp = pps.getPageProvider("bar", ppd, null, null, 1L, 0L, null);
        assertNotNull(pp);
        assertTrue("wrong class " + pp, pp instanceof CoreQueryDocumentPageProvider);
    }

}
