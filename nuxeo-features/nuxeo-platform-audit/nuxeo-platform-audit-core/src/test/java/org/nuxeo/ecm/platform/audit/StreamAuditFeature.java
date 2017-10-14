/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.platform.audit;

import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature.Waiter;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ManagementFeature;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import static org.nuxeo.ecm.platform.audit.impl.StreamAuditWriter.COMPUTATION_NAME;
import static org.nuxeo.ecm.platform.audit.listener.StreamAuditEventListener.STREAM_CONFIG;
import static org.nuxeo.ecm.platform.audit.listener.StreamAuditEventListener.STREAM_NAME;

@Features({ManagementFeature.class, PlatformFeature.class})
@Deploy({"org.nuxeo.runtime.stream", "org.nuxeo.runtime.datasource", "org.nuxeo.runtime.metrics", "org.nuxeo.ecm.core.persistence", "org.nuxeo.ecm.platform.audit"})
@LocalDeploy({"org.nuxeo.ecm.platform.audit:nxaudit-ds.xml", "org.nuxeo.ecm.platform.audit:test-stream-audit-contrib.xml"})
public class StreamAuditFeature extends AuditFeature {

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        runner.getFeature(TransactionalFeature.class).addWaiter(new StreamAuditWaiter());
    }


    protected class StreamAuditWaiter implements Waiter {
        @Override
        public boolean await(long deadline) throws InterruptedException {
            // when there is no lag between producer and consumer we are done
            while (getMQManager().getLag(STREAM_NAME, COMPUTATION_NAME).lag() > 0) {
                if (System.currentTimeMillis() > deadline) {
                    return false;
                }
                Thread.sleep(50);
            }
            return true;
        }
    }

    protected LogManager getMQManager() {
        StreamService service = Framework.getService(StreamService.class);
        return service.getLogManager(STREAM_CONFIG);
    }
}
