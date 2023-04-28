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
import org.opensaml.messaging.handler.MessageHandler;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;

/**
 * @since 2023.0
 */
public class InvokeInitInboundHandlerAction extends AbstractProfileAction {

    private static final Logger log = LogManager.getLogger(InvokeInitInboundHandlerAction.class);

    protected final MessageHandler handler;

    public InvokeInitInboundHandlerAction(MessageHandler handler) {
        this.handler = handler;
    }

    @Override
    protected void doExecute(ProfileRequestContext profileRequestContext) {
        try {
            var inboundMessageContext = profileRequestContext.getInboundMessageContext();
            handler.invoke(inboundMessageContext);
        } catch (MessageHandlerException e) {
            log.info("{} Unable to run handler", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.RUNTIME_EXCEPTION);
        }
    }
}
