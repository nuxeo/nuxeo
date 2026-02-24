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
package org.nuxeo.ecm.platform.auth.saml.processor;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.nuxeo.ecm.platform.auth.saml.SAMLConfiguration;
import org.nuxeo.ecm.platform.auth.saml.processor.action.DecodeInboundRequestAction;
import org.nuxeo.ecm.platform.auth.saml.processor.action.HandleEventContextErrorAction;
import org.nuxeo.ecm.platform.auth.saml.processor.action.HandleSSOMessageAction;
import org.nuxeo.ecm.platform.auth.saml.processor.action.SetNameIdentifierFromResponseAction;
import org.nuxeo.ecm.platform.auth.saml.processor.binding.SAMLInboundBinding;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.handler.MessageHandler;
import org.opensaml.profile.action.ProfileAction;
import org.opensaml.profile.context.PreviousEventContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.context.navigate.SAMLEntityIDFunction;
import org.opensaml.saml.saml2.assertion.SAML20AssertionValidator;
import org.opensaml.saml.saml2.assertion.impl.AudienceRestrictionConditionValidator;
import org.opensaml.saml.saml2.assertion.impl.BearerSubjectConfirmationValidator;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.profile.impl.DecryptAssertions;
import org.opensaml.saml.saml2.profile.impl.DecryptAttributes;
import org.opensaml.saml.saml2.profile.impl.DefaultAssertionValidationContextBuilder;
import org.opensaml.saml.saml2.profile.impl.ValidateAssertions;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.context.SecurityParametersContext;

import net.shibboleth.utilities.java.support.net.HttpServletRequestResponseContext;

/**
 * @since 2023.0
 */
public class InboundProcessor extends AbstractSAMLProcessor {

    protected final SAMLInboundBinding inboundBinding;

    protected final MessageHandler inboundHandler;

    protected final boolean signatureMandatory;

    protected final Duration skewTime;

    /**
     * @deprecated since 2023.44, use
     *             {@link InboundProcessor#InboundProcessor(SAMLInboundBinding, MessageHandler, SAMLConfiguration)}
     *             instead.
     */
    @Deprecated(since = "2023.44", forRemoval = true)
    public InboundProcessor(SAMLInboundBinding inboundBinding, MessageHandler inboundHandler,
            boolean signatureMandatory) {
        this.inboundBinding = inboundBinding;
        this.inboundHandler = inboundHandler;
        this.signatureMandatory = signatureMandatory;
        this.skewTime = SAMLConfiguration.retrieveDefaultPluginConfiguration().getSPSkewTime();
    }

    /**
     * @since 2023.44
     */
    public InboundProcessor(SAMLInboundBinding inboundBinding, MessageHandler inboundHandler,
            SAMLConfiguration configuration) {
        this.inboundBinding = inboundBinding;
        this.inboundHandler = inboundHandler;
        this.signatureMandatory = configuration.isIdPSignatureMandatory();
        this.skewTime = configuration.getSPSkewTime();
    }

    @Override
    protected List<ProfileAction> getActions() {
        return List.of( //
                new DecodeInboundRequestAction(inboundBinding, inboundHandler), //
                new DecryptAssertions(), //
                new DecryptAttributes(), //
                newValidateAssertionsAction(), //
                newSetNameIdentifierAction(), //
                newHandleSSOMessageAction(), //
                // handle event error here because EventContext is cleaned between actions
                new HandleEventContextErrorAction() //
        );
    }

    protected ValidateAssertions newValidateAssertionsAction() {
        var validateAssertions = new ValidateAssertions();
        validateAssertions.setHttpServletRequestSupplier(HttpServletRequestResponseContext::getRequest);
        validateAssertions.setAssertionValidatorLookup(pair -> {
            var trustEngine = pair.getFirst()
                                  .getInboundMessageContext()
                                  .getSubcontext(SecurityParametersContext.class)
                                  .getSignatureValidationParameters()
                                  .getSignatureTrustEngine();
            // newAssertionValidator could be set to AuthnStatementValidator but it wasn't validated before lib upgrade
            return new SAML20AssertionValidator(List.of(new AudienceRestrictionConditionValidator()),
                    List.of(new BearerSubjectConfirmationValidator()), List.of(), null, trustEngine,
                    new SAMLSignatureProfileValidator());
        });
        var validationContextBuilder = new DefaultAssertionValidationContextBuilder();
        validationContextBuilder.setCheckAddress(prc -> false);
        validationContextBuilder.setSignatureRequired(prc -> {
            var peerEntityContext = Optional.ofNullable(
                    prc.getInboundMessageContext().getSubcontext(SAMLPeerEntityContext.class));
            // authenticated field is filled by the response signature validation, in such case don't require assertion
            // signature, nevertheless validate the signature if present in assertion
            return signatureMandatory
                    && peerEntityContext.filter(Predicate.not(SAMLPeerEntityContext::isAuthenticated))
                                        .map(context -> context.getSubcontext(SAMLMetadataContext.class))
                                        .map(SAMLMetadataContext::getRoleDescriptor)
                                        .map(RoleDescriptor::getKeyDescriptors)
                                        .stream()
                                        .flatMap(List::stream)
                                        .anyMatch(keyDescriptor -> keyDescriptor.getUse() == UsageType.SIGNING);
        });
        validationContextBuilder.setInResponseToRequired(prc -> false);
        // we don't handle the inResponse attribute for now, set it to the one present in the response to allow
        // validation
        validationContextBuilder.setInResponseTo(
                prc -> ((Response) prc.getInboundMessageContext().getMessage()).getInResponseTo());
        validationContextBuilder.setClockSkew(skewTime);
        validationContextBuilder.setValidIssuers(prc -> {
            var entityID = new SAMLEntityIDFunction().compose(new ChildContextLookup<>(SAMLPeerEntityContext.class))
                                                     .apply(prc.getInboundMessageContext());
            return entityID == null ? Set.of() : Set.of(entityID);
        });
        validateAssertions.setValidationContextBuilder(validationContextBuilder);
        return validateAssertions;
    }

    protected SetNameIdentifierFromResponseAction newSetNameIdentifierAction() {
        var setNameIdentifierAction = new SetNameIdentifierFromResponseAction();
        setNameIdentifierAction.setActivationCondition(this::isSuccessSSOResponse);
        return setNameIdentifierAction;

    }

    protected HandleSSOMessageAction newHandleSSOMessageAction() {
        var handleSSOAction = new HandleSSOMessageAction();
        handleSSOAction.setActivationCondition(this::isSuccessSSOResponse);
        return handleSSOAction;

    }

    protected boolean isSuccessSSOResponse(ProfileRequestContext prc) {
        var message = prc.getInboundMessageContext().getMessage();
        return !(message instanceof LogoutRequest) && !(message instanceof LogoutResponse)
                && prc.getSubcontext(PreviousEventContext.class) == null;
    }
}
