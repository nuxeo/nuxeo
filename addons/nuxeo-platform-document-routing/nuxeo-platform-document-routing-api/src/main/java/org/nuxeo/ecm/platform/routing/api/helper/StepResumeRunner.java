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
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
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
            DocumentModel parent = session.getParentDocument(model.getRef());
            DocumentRouteElement parentElement = parent.getAdapter(DocumentRouteElement.class);
            parentElement.run(session);
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }
}
