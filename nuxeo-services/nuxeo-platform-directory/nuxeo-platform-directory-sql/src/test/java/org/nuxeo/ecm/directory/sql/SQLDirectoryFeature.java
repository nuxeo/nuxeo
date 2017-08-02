/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mhilaire
 */
package org.nuxeo.ecm.directory.sql;

import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * @since 6.0
 */
@Features({ CoreFeature.class, ClientLoginFeature.class })
@Deploy({ "org.nuxeo.ecm.directory.api", "org.nuxeo.ecm.directory", "org.nuxeo.ecm.core.schema",
        "org.nuxeo.ecm.directory.types.contrib", "org.nuxeo.ecm.directory.sql" })
@LocalDeploy("org.nuxeo.ecm.directory.sql:nxdirectory-ds.xml")
public class SQLDirectoryFeature extends SimpleFeature {

    protected Granularity granularity;

    @Override
    public void beforeRun(FeaturesRunner runner) throws Exception {
        granularity = runner.getFeature(CoreFeature.class).getGranularity();
    }

}
