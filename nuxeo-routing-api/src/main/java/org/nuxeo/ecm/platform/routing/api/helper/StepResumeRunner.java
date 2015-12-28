/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 */
package org.nuxeo.ecm.platform.routing.api.helper;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;

/**
 * Allows to resume a route from the id of a step.
 *
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 */
@Deprecated
public class StepResumeRunner {

    protected String stepDocId;

    public StepResumeRunner(String stepDocId) {
        this.stepDocId = stepDocId;
    }

    public void resumeStep(CoreSession session) {
        DocumentModel model = session.getDocument(new IdRef(stepDocId));
        DocumentRouteStep step = model.getAdapter(DocumentRouteStep.class);
        step.setDone(session);
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                DocumentModel model = session.getDocument(new IdRef(stepDocId));
                DocumentRouteStep step = model.getAdapter(DocumentRouteStep.class);
                DocumentRoute route = step.getDocumentRoute(session);
                route.run(session);
            }
        }.runUnrestricted();
    }
}
