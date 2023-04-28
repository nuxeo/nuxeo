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
package org.nuxeo.ecm.platform.auth.saml.processor.messaging;

import java.util.function.Function;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.RequestAbstractType;
import org.opensaml.saml.saml2.core.StatusResponseType;

/**
 * @since 2023.0
 */
public class SAMLObjectIssuerFunction implements Function<MessageContext, String> {

    @Override
    public String apply(final MessageContext context) {
        if (context == null) {
            return null;
        }
        var message = (SAMLObject) context.getMessage();
        Issuer issuer = null;
        if (message instanceof RequestAbstractType request) {
            issuer = request.getIssuer();
        } else if (message instanceof StatusResponseType responseType) {
            issuer = responseType.getIssuer();
        }
        return issuer != null ? issuer.getValue() : null;
    }
}
