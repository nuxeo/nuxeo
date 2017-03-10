/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import static org.junit.Assume.assumeTrue;

import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * Feature for CMIS tests.
 */
@Features({ CoreFeature.class, AuditFeature.class })
@Deploy({ "org.nuxeo.ecm.directory", //
        "org.nuxeo.ecm.directory.sql", //
        "org.nuxeo.ecm.core.query", //
        "org.nuxeo.ecm.platform.query.api", //
        "org.nuxeo.ecm.platform.ws", //
        "org.nuxeo.ecm.core.io", //
        // deployed for fulltext indexing
        "org.nuxeo.ecm.platform.convert", //
        // MyDocType
        "org.nuxeo.ecm.core.opencmis.tests", //
        // MIME Type Icon Updater for renditions
        "org.nuxeo.ecm.platform.filemanager.api", //
        "org.nuxeo.ecm.platform.filemanager.core", //
        "org.nuxeo.ecm.platform.filemanager.core.listener", //
        "org.nuxeo.ecm.platform.types.api", //
        "org.nuxeo.ecm.platform.types.core", //
        // Rendition Service
        "org.nuxeo.ecm.actions", //
        "org.nuxeo.ecm.platform.rendition.api", //
        "org.nuxeo.ecm.platform.rendition.core", //
        "org.nuxeo.ecm.automation.core", //
        "org.nuxeo.ecm.platform.thumbnail", //
        "org.nuxeo.ecm.platform.url.core", //
        // NuxeoCmisServiceFactoryManager registration
        "org.nuxeo.ecm.core.opencmis.bindings", //
        // QueryMaker registration
        "org.nuxeo.ecm.core.opencmis.impl", //
        // these deployments needed for NuxeoAuthenticationFilter.loginAs
        "org.nuxeo.ecm.directory.types.contrib", //
        "org.nuxeo.ecm.platform.login", //
        "org.nuxeo.ecm.platform.web.common" })
@LocalDeploy({ "org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/validation-contrib.xml", //
        "org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/disable-thumbnail-listener.xml", //
        "org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/disable-filemanager-icon-listener.xml", //
        "org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/disable-collaborative-versioning-policy.xml" })
public class CmisFeature extends SimpleFeature {

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        for (RunnerFeature f : runner.getFeatures()) {
            if (f instanceof CmisFeatureConfiguration) {
                // avoid running a base suite without
                // the actual feature doing the configuration
                return;
            }
        }
        assumeTrue("No contributed CmisFeatureConfiguration", false);
    }

}
