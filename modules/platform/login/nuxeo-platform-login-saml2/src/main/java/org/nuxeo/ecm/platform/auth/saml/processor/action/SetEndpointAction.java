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

import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.auth.saml.processor.binding.SAMLOutboundBinding;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;

/**
 * @since 2023.0
 */
public class SetEndpointAction extends AbstractProfileAction {

    protected final Function<IDPSSODescriptor, List<? extends Endpoint>> endpointResolver;

    protected final SAMLOutboundBinding outboundBinding;

    public SetEndpointAction(Function<IDPSSODescriptor, List<? extends Endpoint>> endpointResolver,
            SAMLOutboundBinding outboundBinding) {
        this.endpointResolver = endpointResolver;
        this.outboundBinding = outboundBinding;
    }

    @Override
    protected void doExecute(@NotNull ProfileRequestContext profileRequestContext) {
        var samlMetadataContext = profileRequestContext.getInboundMessageContext()
                                                       .getSubcontext(SAMLPeerEntityContext.class)
                                                       .getSubcontext(SAMLMetadataContext.class);
        var idpSSODescriptor = (IDPSSODescriptor) samlMetadataContext.getRoleDescriptor();

        var endpoint = endpointResolver.apply(idpSSODescriptor)
                                       .stream()
                                       .filter(e -> outboundBinding.getBindingURI().equals(e.getBinding()))
                                       .findFirst()
                                       .orElseThrow(() -> new NuxeoException(
                                               "The IDP doesn't support the outbound binding: " + outboundBinding));

        var context = profileRequestContext.getOutboundMessageContext();
        var endpointContext = context.getSubcontext(SAMLPeerEntityContext.class, true)
                                     .getSubcontext(SAMLEndpointContext.class, true);
        endpointContext.setEndpoint(endpoint);
    }
}
