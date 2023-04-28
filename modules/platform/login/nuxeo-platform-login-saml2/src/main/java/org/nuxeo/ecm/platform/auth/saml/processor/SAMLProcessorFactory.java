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

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.auth.saml.SAMLConfiguration;
import org.nuxeo.ecm.platform.auth.saml.key.KeyManager;
import org.nuxeo.ecm.platform.auth.saml.processor.binding.SAMLInboundBinding;
import org.nuxeo.ecm.platform.auth.saml.processor.binding.SAMLOutboundBinding;
import org.nuxeo.ecm.platform.auth.saml.processor.handler.PopulateDecryptionParametersHandler;
import org.nuxeo.ecm.platform.auth.saml.processor.messaging.SAMLObjectIssuerFunction;
import org.nuxeo.runtime.api.Framework;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.handler.MessageHandler;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.messaging.handler.impl.BasicMessageHandlerChain;
import org.opensaml.messaging.handler.impl.CheckExpectedIssuer;
import org.opensaml.messaging.handler.impl.CheckMandatoryAuthentication;
import org.opensaml.messaging.handler.impl.FunctionMessageHandler;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.common.binding.impl.PopulateSignatureSigningParametersHandler;
import org.opensaml.saml.common.binding.impl.SAMLMetadataLookupHandler;
import org.opensaml.saml.common.binding.impl.SAMLProtocolAndRoleHandler;
import org.opensaml.saml.common.binding.security.impl.MessageLifetimeSecurityHandler;
import org.opensaml.saml.common.binding.security.impl.SAMLOutboundProtocolMessageSigningHandler;
import org.opensaml.saml.common.binding.security.impl.SAMLProtocolMessageXMLSignatureSecurityHandler;
import org.opensaml.saml.common.messaging.context.AbstractSAMLEntityContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.context.SAMLSelfEntityContext;
import org.opensaml.saml.common.messaging.context.navigate.SAMLMessageContextIssuerFunction;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.metadata.IterableMetadataSource;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.AbstractMetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.FilesystemMetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.HTTPMetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.PredicateRoleDescriptorResolver;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusResponseType;
import org.opensaml.saml.saml2.encryption.EncryptedElementTypeEncryptedKeyResolver;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.security.impl.MetadataCredentialResolver;
import org.opensaml.saml.security.impl.SAMLMetadataSignatureSigningParametersResolver;
import org.opensaml.xmlsec.DecryptionConfiguration;
import org.opensaml.xmlsec.SignatureSigningConfiguration;
import org.opensaml.xmlsec.SignatureValidationConfiguration;
import org.opensaml.xmlsec.config.impl.DefaultSecurityConfigurationBootstrap;
import org.opensaml.xmlsec.encryption.support.ChainingEncryptedKeyResolver;
import org.opensaml.xmlsec.encryption.support.InlineEncryptedKeyResolver;
import org.opensaml.xmlsec.encryption.support.SimpleRetrievalMethodEncryptedKeyResolver;
import org.opensaml.xmlsec.impl.BasicDecryptionParametersResolver;
import org.opensaml.xmlsec.impl.BasicSignatureValidationParametersResolver;
import org.opensaml.xmlsec.keyinfo.impl.StaticKeyInfoCredentialResolver;
import org.opensaml.xmlsec.messaging.impl.PopulateSignatureValidationParametersHandler;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * @since 2023.0
 */
public class SAMLProcessorFactory {

    protected static final String SIGNATURE_ALGORITHM = "SignatureAlgorithm";

    protected static final String DIGEST_ALGORITHM = "DigestAlgorithm";

    /**
     * Message handlers that run on a SAML inbound message, ie: message from IDP.
     */
    protected final MessageHandler inboundHandlerChain;

    /**
     * Message handlers that init the inbound context during SAML outbound message creation, ie: message to IDP.
     */
    protected final MessageHandler initInboundForOutboundHandlerChain;

    /**
     * Message handlers that run on a SAML outbound message, ie: message to IDP.
     */
    protected final MessageHandler outboundHandlerChain;

    public SAMLProcessorFactory(Map<String, String> parameters) {
        try {
            var idpMetadataResolver = instantiateIdpMetadataResolver(parameters);
            var signingConfiguration = instantiateSigningConfiguration(parameters);
            var validationConfiguration = instantiateValidationConfiguration(idpMetadataResolver);
            var decryptionConfiguration = instantiateDecryptionConfiguration();

            var inboundHandlers = new ArrayList<MessageHandler>();
            inboundHandlers.add(buildEntityIdHandler(SAMLConfiguration.getEntityId(), SAMLSelfEntityContext.class));
            inboundHandlers.add(buildSAMLProtocolAndRoleHandler(IDPSSODescriptor.DEFAULT_ELEMENT_NAME));
            inboundHandlers.add(buildSAMLMetadataLookupHandler(idpMetadataResolver));
            inboundHandlers.add(buildMessageLifetimeSecurityHandler());
            inboundHandlers.add(buildCheckExpectedIssuer());
            inboundHandlers.add(buildCheckResponseStatus());
            inboundHandlers.add(buildPopulateSignatureValidationParametersHandler(validationConfiguration));
            inboundHandlers.add(buildSAMLProtocolMessageXMLSignatureSecurityHandler());
            inboundHandlers.add(buildCheckMandatoryAuthentication());
            inboundHandlers.add(buildPopulateDecryptionParametersHandler(decryptionConfiguration));
            inboundHandlerChain = toHandlerChain(inboundHandlers);

            var idpEntryId = ((IterableMetadataSource) idpMetadataResolver).iterator().next().getEntityID();
            var initInboundForOutboundHandlers = new ArrayList<MessageHandler>();
            initInboundForOutboundHandlers.add(buildSAMLProtocolAndRoleHandler(IDPSSODescriptor.DEFAULT_ELEMENT_NAME));
            // needed to retrieve IDP metadata, in inbound case this is retrieved from SAML Message
            initInboundForOutboundHandlers.add(buildEntityIdHandler(idpEntryId, SAMLPeerEntityContext.class));
            initInboundForOutboundHandlers.add(buildSAMLMetadataLookupHandler(idpMetadataResolver));
            initInboundForOutboundHandlerChain = toHandlerChain(initInboundForOutboundHandlers);

            var outboundHandlers = new ArrayList<MessageHandler>();
            outboundHandlers.add(buildSAMLProtocolAndRoleHandler(SPSSODescriptor.DEFAULT_ELEMENT_NAME));
            outboundHandlers.add(buildPopulateSignatureSigningParametersHandler(signingConfiguration));
            outboundHandlers.add(buildSAMLOutboundProtocolMessageSigningHandler());
            outboundHandlerChain = toHandlerChain(outboundHandlers);
        } catch (ComponentInitializationException e) {
            throw new NuxeoException("Unable to init SAML plugin with parameters: " + parameters);
        }
    }

    public Optional<SAMLProcessor> retrieveInboundProcessor(HttpServletRequest request) {
        return Stream.of(SAMLInboundBinding.values())
                     .filter(b -> b.accept(request))
                     .findFirst()
                     .map(b -> new InboundProcessor(b, inboundHandlerChain));
    }

    public SAMLProcessor retrieveOutboundProcessor(String profileId) {
        // @formatter:off old eclipse version doesn't properly format enhanced switch
        return switch (profileId) {
            case SLOOutboundProcessor.PROFILE_URI -> new SLOOutboundProcessor(initInboundForOutboundHandlerChain,
                    outboundHandlerChain, SAMLOutboundBinding.HTTP_REDIRECT);
            case WebSSOOutboundProcessor.PROFILE_URI -> new WebSSOOutboundProcessor(initInboundForOutboundHandlerChain,
                    outboundHandlerChain, SAMLOutboundBinding.HTTP_REDIRECT);
            default -> null;
        };
        // @formatter:on
    }

    protected BasicMessageHandlerChain toHandlerChain(List<MessageHandler> outboundHandlers)
            throws ComponentInitializationException {
        var handler = new BasicMessageHandlerChain();
        handler.setHandlers(outboundHandlers);
        handler.initialize();
        return handler;
    }

    protected MetadataResolver instantiateIdpMetadataResolver(Map<String, String> parameters)
            throws ComponentInitializationException {
        try {
            AbstractMetadataResolver metadataResolver;

            String metadataUrl = parameters.get("metadata");
            if (metadataUrl == null) {
                throw new ResolverException("No metadata URI set for provider: " + parameters.getOrDefault("name", ""));
            }

            if (metadataUrl.startsWith("http:") || metadataUrl.startsWith("https:")) {
                int requestTimeout = Integer.parseInt(parameters.getOrDefault("timeout", "5"));
                int timeoutMs = requestTimeout * 1000;
                var httpClient = HttpClientBuilder.create()
                                                  .setDefaultRequestConfig(
                                                          RequestConfig.custom()
                                                                       .setConnectTimeout(timeoutMs)
                                                                       .setConnectionRequestTimeout(timeoutMs)
                                                                       .setSocketTimeout(timeoutMs)
                                                                       .build())
                                                  .build();
                metadataResolver = new HTTPMetadataResolver(httpClient, metadataUrl);
            } else { // file
                metadataResolver = new FilesystemMetadataResolver(new File(metadataUrl));
            }

            metadataResolver.setId("IDP");
            metadataResolver.setParserPool(XMLObjectProviderRegistrySupport.getParserPool());
            metadataResolver.initialize();
            return metadataResolver;
        } catch (ResolverException e) {
            throw new ComponentInitializationException("Unable to init IDP metadata resolver", e);
        }
    }

    protected SignatureSigningConfiguration instantiateSigningConfiguration(Map<String, String> parameters) {
        if (Framework.getService(KeyManager.class).getSigningCredential() == null) {
            return null;
        } else {
            var signingConfiguration = DefaultSecurityConfigurationBootstrap.buildDefaultSignatureSigningConfiguration();
            signingConfiguration.setSigningCredentials(
                    List.of(Framework.getService(KeyManager.class).getSigningCredential()));
            if (parameters.containsKey(DIGEST_ALGORITHM)) {
                signingConfiguration.setSignatureReferenceDigestMethods(List.of(parameters.get(DIGEST_ALGORITHM)));
            }
            // TODO handle algo not known to the library?
            var algorithms = parameters.entrySet()
                                       .stream()
                                       .filter(e -> e.getKey().startsWith(SIGNATURE_ALGORITHM))
                                       .map(Entry::getValue)
                                       .collect(Collectors.toList());
            if (!algorithms.isEmpty()) {
                signingConfiguration.setSignatureAlgorithms(algorithms);
            }
            return signingConfiguration;
        }
    }

    protected SignatureValidationConfiguration instantiateValidationConfiguration(MetadataResolver idpMetadataResolver)
            throws ComponentInitializationException {
        var roleResolver = new PredicateRoleDescriptorResolver(idpMetadataResolver);
        var keyInfoResolver = DefaultSecurityConfigurationBootstrap.buildBasicInlineKeyInfoCredentialResolver();
        var metadataCredentialResolver = new MetadataCredentialResolver();
        metadataCredentialResolver.setRoleDescriptorResolver(roleResolver);
        metadataCredentialResolver.setKeyInfoCredentialResolver(keyInfoResolver);
        roleResolver.initialize();
        metadataCredentialResolver.initialize();
        var trustEngine = new ExplicitKeySignatureTrustEngine(metadataCredentialResolver, keyInfoResolver);

        var validationConfiguration = DefaultSecurityConfigurationBootstrap.buildDefaultSignatureValidationConfiguration();
        validationConfiguration.setSignatureTrustEngine(trustEngine);
        return validationConfiguration;
    }

    protected DecryptionConfiguration instantiateDecryptionConfiguration() {
        if (Framework.getService(KeyManager.class).getEncryptionCredential() == null) {
            return null;
        } else {
            var encryptionCredential = Framework.getService(KeyManager.class).getEncryptionCredential();

            var decryptionConfiguration = DefaultSecurityConfigurationBootstrap.buildDefaultDecryptionConfiguration();
            decryptionConfiguration.setEncryptedKeyResolver(new ChainingEncryptedKeyResolver(List.of( //
                    new InlineEncryptedKeyResolver(), //
                    new EncryptedElementTypeEncryptedKeyResolver(), //
                    new SimpleRetrievalMethodEncryptedKeyResolver() //
            )));
            decryptionConfiguration.setKEKKeyInfoCredentialResolver(
                    new StaticKeyInfoCredentialResolver(encryptionCredential));
            return decryptionConfiguration;
        }
    }

    /**
     * Builds handler to populate peer.
     */
    protected MessageHandler buildSAMLProtocolAndRoleHandler(QName roleName) throws ComponentInitializationException {
        var protocolAndRoleHandler = new SAMLProtocolAndRoleHandler();
        protocolAndRoleHandler.setProtocol(SAMLConstants.SAML20P_NS);
        protocolAndRoleHandler.setRole(roleName);
        protocolAndRoleHandler.initialize();
        return protocolAndRoleHandler;
    }

    /**
     * Builds handler to populate peer.
     */
    protected <C extends AbstractSAMLEntityContext> MessageHandler buildEntityIdHandler(String entityId,
            Class<C> contextClass) throws ComponentInitializationException {
        var entityIdHandler = new FunctionMessageHandler();
        entityIdHandler.setFunction(context -> {
            var peerEntityContext = context.getSubcontext(contextClass, true);
            if (peerEntityContext.getEntityId() == null) {
                peerEntityContext.setEntityId(entityId);
            }
            return null;
        });
        entityIdHandler.initialize();
        return entityIdHandler;
    }

    protected MessageHandler buildSAMLMetadataLookupHandler(MetadataResolver metadataResolver)
            throws ComponentInitializationException {
        var roleResolver = new PredicateRoleDescriptorResolver(metadataResolver);
        roleResolver.initialize();

        var metadataLookupHandler = new SAMLMetadataLookupHandler();
        metadataLookupHandler.setRoleDescriptorResolver(roleResolver);
        metadataLookupHandler.initialize();
        return metadataLookupHandler;
    }

    protected MessageHandler buildMessageLifetimeSecurityHandler() throws ComponentInitializationException {
        var lifetimeHandler = new MessageLifetimeSecurityHandler();
        lifetimeHandler.setClockSkew(Duration.ofMillis(SAMLConfiguration.getSkewTimeMillis()));
        lifetimeHandler.initialize();
        return lifetimeHandler;
    }

    protected MessageHandler buildCheckExpectedIssuer() throws ComponentInitializationException {
        var expectedIssuer = new CheckExpectedIssuer();
        expectedIssuer.setIssuerLookupStrategy(new SAMLObjectIssuerFunction());
        // function will look for data set by SAMLMetadataLookupHandler
        expectedIssuer.setExpectedIssuerLookupStrategy(new SAMLMessageContextIssuerFunction());
        expectedIssuer.initialize();
        return expectedIssuer;
    }

    protected MessageHandler buildCheckResponseStatus() throws ComponentInitializationException {
        var checkResponseHandler = new FunctionMessageHandler();
        checkResponseHandler.setFunction(context -> {
            Object message = context.getMessage();
            if (message instanceof StatusResponseType statusResponseType) {
                var status = statusResponseType.getStatus();
                String statusCode = status.getStatusCode().getValue();
                if (!StatusCode.SUCCESS.equals(statusCode) && !StatusCode.PARTIAL_LOGOUT.equals(statusCode)) {
                    return new MessageHandlerException(
                            String.format("The received status code is not a success, code: %s, message: %s",
                                    statusCode, status.getStatusMessage()));
                }
            }
            return null;
        });
        checkResponseHandler.initialize();
        return checkResponseHandler;
    }

    protected MessageHandler buildPopulateSignatureValidationParametersHandler(
            SignatureValidationConfiguration validationConfiguration) throws ComponentInitializationException {
        var signatureValidationParameters = new PopulateSignatureValidationParametersHandler();
        signatureValidationParameters.setConfigurationLookupStrategy(context -> List.of(validationConfiguration));
        signatureValidationParameters.setSignatureValidationParametersResolver(
                new BasicSignatureValidationParametersResolver());
        signatureValidationParameters.initialize();
        return signatureValidationParameters;
    }

    protected MessageHandler buildSAMLProtocolMessageXMLSignatureSecurityHandler()
            throws ComponentInitializationException {
        var messageXMLSignatureHandler = new SAMLProtocolMessageXMLSignatureSecurityHandler();
        messageXMLSignatureHandler.initialize();
        return messageXMLSignatureHandler;
    }

    protected MessageHandler buildCheckMandatoryAuthentication() {
        var mandatoryAuthentication = new CheckMandatoryAuthentication();
        mandatoryAuthentication.setAuthenticationLookupStrategy(
                context -> ((SignableSAMLObject) context.getMessage()).getSignature() == null
                        || context.getSubcontext(SAMLPeerEntityContext.class).isAuthenticated());
        return mandatoryAuthentication;
    }

    protected MessageHandler buildPopulateDecryptionParametersHandler(DecryptionConfiguration decryptionConfiguration)
            throws ComponentInitializationException {
        var decryptionParameters = new PopulateDecryptionParametersHandler();
        decryptionParameters.setActivationCondition(context -> decryptionConfiguration != null);
        decryptionParameters.setConfigurationLookupStrategy(context -> List.of(decryptionConfiguration));
        decryptionParameters.setDecryptionParametersResolver(new BasicDecryptionParametersResolver());
        decryptionParameters.initialize();
        return decryptionParameters;
    }

    protected MessageHandler buildPopulateSignatureSigningParametersHandler(
            SignatureSigningConfiguration signingConfiguration) throws ComponentInitializationException {
        var signatureSigningParameters = new PopulateSignatureSigningParametersHandler();
        signatureSigningParameters.setActivationCondition(context -> signingConfiguration != null);
        signatureSigningParameters.setConfigurationLookupStrategy(context -> List.of(signingConfiguration));
        signatureSigningParameters.setSignatureSigningParametersResolver(
                new SAMLMetadataSignatureSigningParametersResolver());
        signatureSigningParameters.setNoResultIsError(true);
        signatureSigningParameters.initialize();
        return signatureSigningParameters;
    }

    protected MessageHandler buildSAMLOutboundProtocolMessageSigningHandler() throws ComponentInitializationException {
        var messageSigner = new SAMLOutboundProtocolMessageSigningHandler();
        messageSigner.initialize();
        return messageSigner;
    }
}
