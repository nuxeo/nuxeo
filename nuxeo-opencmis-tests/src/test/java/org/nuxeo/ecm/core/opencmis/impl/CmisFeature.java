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

import org.junit.AssumptionViolatedException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * Feature for CMIS tests.
 */
@Features({ CoreFeature.class, AuditFeature.class })
@Deploy("org.nuxeo.ecm.directory")
@Deploy("org.nuxeo.ecm.directory.sql")
@Deploy("org.nuxeo.ecm.core.query")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.ws")
@Deploy("org.nuxeo.ecm.core.io")
// deployed for fulltext indexing
@Deploy("org.nuxeo.ecm.platform.convert")
// MyDocType
@Deploy("org.nuxeo.ecm.core.opencmis.tests")
// MIME Type Icon Updater for renditions
@Deploy("org.nuxeo.ecm.platform.filemanager.api")
@Deploy("org.nuxeo.ecm.platform.filemanager.core")
@Deploy("org.nuxeo.ecm.platform.filemanager.core.listener")
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.types.core")
// Rendition Service
@Deploy("org.nuxeo.ecm.actions")
@Deploy("org.nuxeo.ecm.platform.rendition.api")
@Deploy("org.nuxeo.ecm.platform.rendition.core")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.thumbnail")
@Deploy("org.nuxeo.ecm.platform.url.core")
// NuxeoCmisServiceFactoryManager registration
@Deploy("org.nuxeo.ecm.core.opencmis.bindings")
// QueryMaker registration
@Deploy("org.nuxeo.ecm.core.opencmis.impl")
// these deployments needed for NuxeoAuthenticationFilter.loginAs
@Deploy("org.nuxeo.ecm.directory.types.contrib")
@Deploy("org.nuxeo.ecm.platform.login")
@Deploy("org.nuxeo.ecm.platform.web.common")
@Deploy("org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/validation-contrib.xml")
@Deploy("org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/disable-thumbnail-listener.xml")
@Deploy("org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/disable-filemanager-icon-listener.xml")
public class CmisFeature implements RunnerFeature {

    @Override
    public void initialize(FeaturesRunner runner) {
        for (RunnerFeature f : runner.getFeatures()) {
            if (f instanceof CmisFeatureConfiguration) {
                // avoid running a base suite without
                // the actual feature doing the configuration
                return;
            }
        }
        throw new AssumptionViolatedException("No contributed CmisFeatureConfiguration");
    }

}
