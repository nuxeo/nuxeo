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
import org.nuxeo.ecm.platform.auth.saml.processor.binding.SAMLInboundBinding;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.handler.MessageHandler;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.net.HttpServletRequestResponseContext;

/**
 * Action that decodes an inbound request from the {@link HttpServletRequestResponseContext} to {@link MessageContext}.
 *
 * @see EventIds#UNABLE_TO_DECODE
 * @since 2023.0
 */
public class DecodeInboundRequestAction extends AbstractProfileAction {

    private static final Logger log = LogManager.getLogger(DecodeInboundRequestAction.class);

    /**
     * The binding to use to obtain a decoder.
     */
    protected SAMLInboundBinding decoderBinding;

    /**
     * Called after decode.
     */
    protected MessageHandler messageHandler;

    public DecodeInboundRequestAction(SAMLInboundBinding decoderBinding, MessageHandler messageHandler) {
        this.decoderBinding = decoderBinding;
        this.messageHandler = messageHandler;
    }

    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (decoderBinding == null) {
            throw new ComponentInitializationException("SAMLInboundBinding cannot be null");
        }
        if (HttpServletRequestResponseContext.getResponse() == null) {
            throw new ComponentInitializationException("HttpServletRequestResponseContext must be loaded");
        }
    }

    @Override
    protected void doExecute(ProfileRequestContext profileRequestContext) {
        var decoder = decoderBinding.newDecoder();
        if (decoder == null) {
            log.error("{} Unable to locate an inbound message decoder", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.UNABLE_TO_DECODE);
            return;
        }

        try {
            log.debug("{} Decoding inbound request using message decoder of type {} for this response", getLogPrefix(),
                    decoder.getClass().getName());
            decoder.setHttpServletRequestSupplier(HttpServletRequestResponseContext::getRequest);
            decoder.initialize();

            decoder.decode();

            var msgContext = decoder.getMessageContext();
            log.debug("{} Invoking message handler of type {} for this request", getLogPrefix(),
                    messageHandler.getClass().getName());
            messageHandler.invoke(msgContext);

            profileRequestContext.setInboundMessageContext(msgContext);
        } catch (MessageDecodingException | ComponentInitializationException | MessageHandlerException e) {
            log.error("{} Unable to decode inbound response", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.UNABLE_TO_DECODE);
        } finally {
            decoder.destroy();
        }
    }

}
