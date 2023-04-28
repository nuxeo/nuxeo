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
package org.nuxeo.ecm.platform.auth.saml.processor.handler;

import java.util.List;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.handler.AbstractMessageHandler;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.xmlsec.DecryptionConfiguration;
import org.opensaml.xmlsec.DecryptionParameters;
import org.opensaml.xmlsec.DecryptionParametersResolver;
import org.opensaml.xmlsec.SecurityConfigurationSupport;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.criterion.DecryptionConfigurationCriterion;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * @since 2023.0
 */
public class PopulateDecryptionParametersHandler extends AbstractMessageHandler {

    /** Class logger. */
    private static final Logger log = LogManager.getLogger(PopulateDecryptionParametersHandler.class);

    /** Strategy used to look up the {@link SecurityParametersContext} to set the parameters for. */
    protected Function<MessageContext, SecurityParametersContext> securityParametersContextLookupStrategy;

    /** Strategy used to lookup a per-request {@link DecryptionConfiguration} list. */
    protected Function<MessageContext, List<DecryptionConfiguration>> configurationLookupStrategy;

    /** Resolver for parameters to store into context. */
    protected DecryptionParametersResolver resolver;

    /**
     * Constructor.
     */
    public PopulateDecryptionParametersHandler() {
        // Create context by default.
        securityParametersContextLookupStrategy = new ChildContextLookup<>(SecurityParametersContext.class, true);
    }

    /**
     * Set the strategy used to look up the {@link SecurityParametersContext} to set the parameters for.
     *
     * @param strategy lookup strategy
     */
    public void setSecurityParametersContextLookupStrategy(
            Function<MessageContext, SecurityParametersContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        securityParametersContextLookupStrategy = Constraint.isNotNull(strategy,
                "SecurityParametersContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to look up a per-request {@link DecryptionConfiguration} list.
     *
     * @param strategy lookup strategy
     */
    public void setConfigurationLookupStrategy(Function<MessageContext, List<DecryptionConfiguration>> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        configurationLookupStrategy = Constraint.isNotNull(strategy,
                "DecryptionConfiguration lookup strategy cannot be null");
    }

    /**
     * Set the resolver to use for the parameters to store into the context.
     *
     * @param newResolver resolver to use
     */
    public void setDecryptionParametersResolver(DecryptionParametersResolver newResolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        resolver = Constraint.isNotNull(newResolver, "DecryptionParametersResolver cannot be null");
    }

    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (resolver == null) {
            throw new ComponentInitializationException("DecryptionParametersResolver cannot be null");
        } else if (configurationLookupStrategy == null) {
            configurationLookupStrategy = input -> List.of(
                    SecurityConfigurationSupport.getGlobalDecryptionConfiguration());
        }
    }

    @Override
    protected void doInvoke(MessageContext messageContext) throws MessageHandlerException {

        log.debug("{} Resolving DecryptionParameters for request", getLogPrefix());

        List<DecryptionConfiguration> configs = configurationLookupStrategy.apply(messageContext);
        if (configs == null || configs.isEmpty()) {
            log.error("{} No DecryptionConfiguration returned by lookup strategy", getLogPrefix());
            throw new MessageHandlerException("No DecryptionConfiguration returned by lookup strategy");
        }

        SecurityParametersContext paramsCtx = securityParametersContextLookupStrategy.apply(messageContext);
        if (paramsCtx == null) {
            log.debug("{} No SecurityParametersContext returned by lookup strategy", getLogPrefix());
            throw new MessageHandlerException("SecurityParametersContext returned by lookup strategy");
        }

        try {
            DecryptionParameters params = resolver.resolveSingle(
                    new CriteriaSet(new DecryptionConfigurationCriterion(configs)));
            paramsCtx.setDecryptionParameters(params);
            log.debug("{} {} DecryptionParameters", getLogPrefix(), params != null ? "Resolved" : "Failed to resolve");
        } catch (ResolverException e) {
            log.error("{} Error resolving DecryptionParameters: {}", getLogPrefix(), e.getMessage());
            throw new MessageHandlerException("Error resolving DecryptionParameters");
        }
    }
}
