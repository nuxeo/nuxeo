/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.platform.auth.saml.processor.action;

import org.jetbrains.annotations.NotNull;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.opensaml.profile.action.AbstractConditionalProfileAction;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLSubjectNameIdentifierContext;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Subject;

/**
 * @since 2023.0
 */
public class SetNameIdentifierFromResponseAction extends AbstractConditionalProfileAction {

    @Override
    protected void doExecute(@NotNull ProfileRequestContext profileRequestContext) {
        var message = profileRequestContext.getInboundMessageContext().getMessage();
        if (message instanceof Response response) {
            var nameID = response.getAssertions()
                                 .stream()
                                 .map(Assertion::getSubject)
                                 .map(Subject::getNameID)
                                 .findFirst()
                                 .orElseThrow(
                                         () -> new NuxeoException("Unable to retrieve the nameID from Assertions"));
            profileRequestContext.getInboundMessageContext()
                                 .getSubcontext(SAMLSubjectNameIdentifierContext.class, true)
                                 .setSubjectNameIdentifier(nameID);
        }
    }
}
