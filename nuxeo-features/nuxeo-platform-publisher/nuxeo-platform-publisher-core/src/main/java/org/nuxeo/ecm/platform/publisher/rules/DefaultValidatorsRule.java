/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Julien Anguenot
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.publisher.rules;

import java.util.Arrays;
import java.util.HashSet;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * Default NXP validator.
 * <p>
 * Validators here will be principals having manage everything rights in the
 * sections where the document has been published.
 */
public class DefaultValidatorsRule implements ValidatorsRule {

    private static final long serialVersionUID = 1L;

    public String[] computesValidatorsFor(DocumentModel doc)
            throws PublishingValidatorException {
        UnrestrictedACPGetter acpg = new UnrestrictedACPGetter(doc);
        try {
            acpg.runUnrestricted();
        } catch (ClientException e) {
            throw new PublishingValidatorException(e);
        }
        String[] writePermissions = doc.getCoreSession().getPermissionsToCheck(
                SecurityConstants.WRITE);
        String[] reviewers = acpg.acp.listUsernamesForAnyPermission(new HashSet<String>(
                Arrays.asList(writePermissions)));
        return reviewers;
    }

    protected static class UnrestrictedACPGetter extends
            UnrestrictedSessionRunner {

        public final DocumentRef docRef;

        public ACP acp;

        public UnrestrictedACPGetter(DocumentModel doc) {
            super(doc.getCoreSession());
            this.docRef = doc.getRef();
        }

        @Override
        public void run() throws ClientException {
            acp = session.getACP(docRef);
        }
    }

}
