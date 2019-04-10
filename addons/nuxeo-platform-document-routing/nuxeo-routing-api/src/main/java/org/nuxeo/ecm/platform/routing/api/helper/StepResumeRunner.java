/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Alexandre Russel
 */
package org.nuxeo.ecm.platform.routing.api.helper;

import org.nuxeo.ecm.core.api.ClientException;
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
 *
 */
@Deprecated
public class StepResumeRunner {

    protected String stepDocId;

    public StepResumeRunner(String stepDocId) {
        this.stepDocId = stepDocId;
    }

    public void resumeStep(CoreSession session) {
        try {
            DocumentModel model = session.getDocument(new IdRef(stepDocId));
            DocumentRouteStep step = model.getAdapter(DocumentRouteStep.class);
            step.setDone(session);
            new UnrestrictedSessionRunner(session) {
                @Override
                public void run() throws ClientException {
                    DocumentModel model = session.getDocument(new IdRef(
                            stepDocId));
                    DocumentRouteStep step = model.getAdapter(DocumentRouteStep.class);
                    DocumentRoute route = step.getDocumentRoute(session);
                    route.run(session);
                }
            }.runUnrestricted();
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }
}
