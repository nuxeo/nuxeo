/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.api.helper;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

/***
 * Set the position of the child to be run once the step with the given id it's
 * finished.
 * 
 * @since 5.5
 */
public class ConditionalFolderUpdateRunner {

    protected String stepDocId;

    public ConditionalFolderUpdateRunner(String stepDocId) {
        this.stepDocId = stepDocId;
    }

    public void setStepToBeExecutedNext(CoreSession session,
            final String nextStepPos) {
        try {
            new UnrestrictedSessionRunner(session) {

                @Override
                public void run() throws ClientException {
                    // get the parent container and set on it the id of the doc
                    // to be run next
                    DocumentModel condFolder = session.getDocument(session.getParentDocumentRef(new IdRef(
                            stepDocId)));
                    if (!condFolder.hasFacet(DocumentRoutingConstants.CONDITIONAL_STEP_FACET)) {
                        return;
                    }
                    condFolder.setPropertyValue(
                            DocumentRoutingConstants.STEP_TO_BE_EXECUTED_NEXT_PROPERTY_NAME,
                            nextStepPos);
                    session.saveDocument(condFolder);
                }
            }.runUnrestricted();
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }
}
