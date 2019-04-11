/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Anguenot
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.publisher.rules;

import java.util.Arrays;
import java.util.HashSet;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * Default NXP validator.
 * <p>
 * Validators here will be principals having manage everything rights in the sections where the document has been
 * published.
 */
public class DefaultValidatorsRule implements ValidatorsRule {

    private static final long serialVersionUID = 1L;

    @Override
    public String[] computesValidatorsFor(DocumentModel doc) {
        UnrestrictedACPGetter acpg = new UnrestrictedACPGetter(doc);
        acpg.runUnrestricted();
        String[] writePermissions = doc.getCoreSession().getPermissionsToCheck(SecurityConstants.WRITE);
        String[] reviewers = acpg.acp.listUsernamesForAnyPermission(new HashSet<>(Arrays.asList(writePermissions)));
        return reviewers;
    }

    protected static class UnrestrictedACPGetter extends UnrestrictedSessionRunner {

        public final DocumentRef docRef;

        public ACP acp;

        public UnrestrictedACPGetter(DocumentModel doc) {
            super(doc.getCoreSession());
            this.docRef = doc.getRef();
        }

        @Override
        public void run() {
            acp = session.getACP(docRef);
        }
    }

}
