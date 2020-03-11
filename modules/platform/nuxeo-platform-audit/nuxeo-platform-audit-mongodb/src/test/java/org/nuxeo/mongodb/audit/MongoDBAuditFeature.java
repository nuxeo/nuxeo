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

import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.core.mongodb.seqgen.MongoDBUIDSequencer;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mongodb.MongoDBFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * @since 9.1
 */
@Deploy("org.nuxeo.ecm.core.event")
@Deploy("org.nuxeo.ecm.core")
@Deploy("org.nuxeo.ecm.platform.audit.api")
@Deploy("org.nuxeo.ecm.platform.audit")
@Deploy("org.nuxeo.mongodb.audit")
@Deploy("org.nuxeo.mongodb.audit.test")
@Features({ MongoDBFeature.class, CoreFeature.class })
public class MongoDBAuditFeature implements RunnerFeature {

    @Override
    public void testCreated(Object test) throws Exception {
        // make sure nothing is currently waiting to be processed
        Framework.getService(AuditLogger.class).await(10, TimeUnit.SECONDS);
        // clean the audit
        MongoDBAuditBackend auditBackend = (MongoDBAuditBackend) Framework.getService(AuditReader.class);
        auditBackend.getAuditCollection().drop();
        // clean the sequence
        MongoDBUIDSequencer uidSeq = (MongoDBUIDSequencer) Framework.getService(UIDSequencer.class);
        uidSeq.getSequencerCollection().drop();
    }

}
