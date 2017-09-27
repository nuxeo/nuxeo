/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.mongodb.audit;

import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.mongodb.MongoDBFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * @since 9.1
 */
@Deploy({ "org.nuxeo.ecm.core.event", //
        "org.nuxeo.ecm.core", //
        "org.nuxeo.ecm.platform.audit.api", //
        "org.nuxeo.ecm.platform.audit", //
        "org.nuxeo.mongodb.audit", //
        "org.nuxeo.mongodb.audit.test", //
})
@Features({ CoreFeature.class, MongoDBFeature.class })
@LocalDeploy({ "org.nuxeo.mongodb.audit.test:OSGI-INF/mongodb-audit-test-contrib.xml" })
public class MongoDBAuditFeature extends SimpleFeature {

}
