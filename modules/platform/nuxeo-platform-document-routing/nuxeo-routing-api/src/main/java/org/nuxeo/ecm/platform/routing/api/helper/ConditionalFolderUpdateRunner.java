/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.api.helper;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

/***
 * Set the position of the child to be run once the step with the given id it's finished.
 *
 * @since 5.5
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 */
@Deprecated
public class ConditionalFolderUpdateRunner {

    protected String stepDocId;

    public ConditionalFolderUpdateRunner(String stepDocId) {
        this.stepDocId = stepDocId;
    }

    public void setStepToBeExecutedNext(CoreSession session, final String nextStepPos) {
        new UnrestrictedSessionRunner(session) {

            @Override
            public void run() {
                // get the parent container and set on it the id of the doc
                // to be run next
                DocumentModel condFolder = session.getDocument(session.getParentDocumentRef(new IdRef(stepDocId)));
                if (!condFolder.hasFacet(DocumentRoutingConstants.CONDITIONAL_STEP_FACET)) {
                    return;
                }
                condFolder.setPropertyValue(DocumentRoutingConstants.STEP_TO_BE_EXECUTED_NEXT_PROPERTY_NAME,
                        nextStepPos);
                session.saveDocument(condFolder);
            }
        }.runUnrestricted();
    }
}
