/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

/**
 * @since 6.0
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.query.api.test:test-pageprovider-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.query.api.test:test-pageprovider-classreplacer-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.query.api.test:test-schemas-contrib.xml")
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
