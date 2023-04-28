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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.auth.saml.processor.binding.SAMLOutboundBinding;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncoder;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.messaging.handler.MessageHandler;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.net.HttpServletRequestResponseContext;

/**
 * Action that encodes an outbound response from the outbound {@link MessageContext}.
 *
 * @implNote copied/inspired from {@link org.opensaml.profile.action.impl.EncodeMessage}
 * @see EventIds#INVALID_MSG_CTX
 * @see EventIds#UNABLE_TO_ENCODE
 * @since 2023.0
 */
public class EncodeOutboundResponseAction extends AbstractProfileAction {

    private static final Logger log = LogManager.getLogger(EncodeOutboundResponseAction.class);

    /**
     * The binding to use to obtain an encoder.
     */
    protected SAMLOutboundBinding encoderBinding;

    /**
     * A {@link MessageHandler} instance to be invoked after {@link MessageEncoder#prepareContext()} and prior to
     * {@link MessageEncoder#encode()}.
     */
    protected MessageHandler messageHandler;

    public EncodeOutboundResponseAction(SAMLOutboundBinding encoderBinding, MessageHandler messageHandler) {
        this.encoderBinding = encoderBinding;
        this.messageHandler = messageHandler;
    }

    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (encoderBinding == null) {
            throw new ComponentInitializationException("SAMLOutboundBinding cannot be null");
        }
    }

    @Override
    protected boolean doPreExecute(ProfileRequestContext profileRequestContext) {
        var msgContext = profileRequestContext.getOutboundMessageContext();
        if (msgContext == null) {
            log.debug("{} Outbound message context was null", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        }

        return super.doPreExecute(profileRequestContext);
    }

    @Override
    protected void doExecute(ProfileRequestContext profileRequestContext) {
        var encoder = encoderBinding.newEncoder();
        if (encoder == null) {
            log.error("{} Unable to locate an outbound message encoder", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.UNABLE_TO_ENCODE);
            return;
        }

        var msgContext = profileRequestContext.getOutboundMessageContext();
        try {
            log.debug("{} Encoding outbound response using message encoder of type {} for this response",
                    getLogPrefix(), encoder.getClass().getName());
            encoder.setMessageContext(msgContext);
            encoder.setHttpServletResponseSupplier(HttpServletRequestResponseContext::getResponse);
            encoder.initialize();

            encoder.prepareContext();

            log.debug("{} Invoking message handler of type {} for this response", getLogPrefix(),
                    messageHandler.getClass().getName());
            messageHandler.invoke(msgContext);

            encoder.encode();

            if (msgContext.getMessage() != null) {
                log.debug("{} Outbound message encoded from a message of type {}", getLogPrefix(),
                        msgContext.getMessage().getClass().getName());
            } else {
                log.debug("{} Outbound message was encoded from protocol-specific data "
                        + "rather than MessageContext#getMessage()", getLogPrefix());
            }

        } catch (MessageEncodingException | ComponentInitializationException | MessageHandlerException e) {
            log.error("{} Unable to encode outbound response", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.UNABLE_TO_ENCODE);
        } finally {
            encoder.destroy();
        }
    }
}
