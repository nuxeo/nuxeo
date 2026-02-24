/*
 * (C) Copyright 2015-2023 Nuxeo (http://nuxeo.com/) and others.
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
 *      Nelson Silva
 */
package org.nuxeo.ecm.platform.auth.saml;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.nuxeo.ecm.platform.auth.saml.SAMLUtils.buildSAMLObject;
import static org.nuxeo.ecm.platform.auth.saml.key.KeyDescriptor.DEFAULT_NAME;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.auth.saml.key.KeyHolder;
import org.nuxeo.ecm.platform.auth.saml.key.KeyManager;
import org.nuxeo.ecm.platform.auth.saml.processor.binding.SAMLInboundBinding;
import org.nuxeo.ecm.platform.auth.saml.user.AbstractUserResolver;
import org.nuxeo.ecm.platform.auth.saml.user.EmailBasedUserResolver;
import org.nuxeo.ecm.platform.auth.saml.user.UserMapperBasedResolver;
import org.nuxeo.ecm.platform.auth.saml.user.UserResolver;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.usermapper.service.UserMapperService;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.NameIDFormat;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.config.impl.DefaultSecurityConfigurationBootstrap;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.signature.KeyInfo;

/**
 * Configuration class that retrieves the SAML configuration from plugin {@link #parameters}.
 * <p>
 * {@code SP} refers to Service Provider (here Nuxeo) and {@code IdP} refers to Identity Provider.
 * 
 * @since 7.3
 */
public class SAMLConfiguration {

    private static final Logger log = LogManager.getLogger(SAMLConfiguration.class);

    public static final String ENTITY_ID = "nuxeo.saml2.entityId";

    public static final String LOGIN_BINDINGS = "nuxeo.saml2.loginBindings";

    public static final String AUTHN_REQUESTS_SIGNED = "nuxeo.saml2.authnRequestsSigned";

    public static final String WANT_ASSERTIONS_SIGNED = "nuxeo.saml2.wantAssertionsSigned";

    public static final String SKEW_TIME_MS = "nuxeo.saml2.skewTimeMs";

    public static final int DEFAULT_SKEW_TIME_MS = 1000 * 60; // 1 minute;

    public static final String BINDING_PREFIX = "urn:oasis:names:tc:SAML:2.0:bindings";

    public static final String DEFAULT_LOGIN_BINDINGS = "HTTP-Redirect,HTTP-POST";

    public static final Collection<String> nameID = Arrays.asList(NameIDType.EMAIL, NameIDType.TRANSIENT,
            NameIDType.PERSISTENT, NameIDType.UNSPECIFIED, NameIDType.X509_SUBJECT);

    protected static final String PARAMETER_ENTITY_ID = "entityId";

    protected static final String PARAMETER_AUTHN_REQUESTS_SIGNED = "authnRequestsSigned";

    protected static final String PARAMETER_WANT_ASSERTIONS_SIGNED = "wantAssertionsSigned";

    protected static final String PARAMETER_LOGIN_BINDINGS = "loginBindings";

    protected static final String PARAMETER_DIGEST_ALGORITHM = "DigestAlgorithm";

    protected static final String PARAMETER_SIGNATURE_ALGORITHM = "SignatureAlgorithm";

    protected static final String PARAMETER_SIGNATURE_MANDATORY = "signatureMandatory";

    protected static final String PARAMETER_SKEW_TIME = "skewTime";

    protected static final String PARAMETER_LOGIN_SCREEN_NAME = "name";

    protected static final String PARAMETER_LOGIN_SCREEN_DESCRIPTION = "description";

    protected static final String PARAMETER_LOGIN_SCREEN_ICON = "icon";

    protected static final String PARAMETER_LOGIN_SCREEN_LABEL = "label";

    protected static final String PARAMETER_KEY_HOLDER_NAME = "keyHolderName";

    protected static final String PARAMETER_USER_RESOLVER_CLASS = "userResolverClass";

    protected static final Class<? extends UserResolver> DEFAULT_USER_RESOLVER_CLASS = EmailBasedUserResolver.class;

    protected static final Class<? extends UserResolver> USERMAPPER_USER_RESOLVER_CLASS = UserMapperBasedResolver.class;

    protected final Map<String, String> parameters;

    public SAMLConfiguration(Map<String, String> parameters) {
        this.parameters = Objects.requireNonNull(parameters, "Parameters must not be null");
    }

    /**
     * The plugin defines as the default is:
     * <ul>
     * <li>the one not declaring {@link #PARAMETER_ENTITY_ID entityId} parameter
     * <li>the one having the {@link #PARAMETER_ENTITY_ID entityId} parameter equals to the {@code nuxeo.conf} parameter
     * </ul>
     * 
     * @return whether this plugin is the default one
     */
    public boolean isDefault() {
        String entityIdParameter = parameters.get(PARAMETER_ENTITY_ID);
        return isBlank(entityIdParameter) || entityIdParameter.equals(getSPEntityIdFromNuxeoConf());
    }

    /**
     * @return the SAML entityId to use for this plugin
     * @since 2023.44
     */
    @Nonnull
    public String getSPEntityId() {
        String entityId = parameters.get(PARAMETER_ENTITY_ID);
        if (isBlank(entityId)) {
            entityId = getSPEntityIdFromNuxeoConf();
        }
        return entityId;
    }

    /**
     * @since 2023.44
     */
    protected String getSPEntityIdFromNuxeoConf() {
        return Framework.getProperty(ENTITY_ID, Framework.getProperty("nuxeo.url"));
    }

    /**
     * @return whether the SP signed the authn requests.
     * @since 2023.44
     */
    public boolean isSPAuthnRequestsSigned() {
        return Boolean.parseBoolean(parameters.get(PARAMETER_AUTHN_REQUESTS_SIGNED))
                || Boolean.parseBoolean(Framework.getProperty(AUTHN_REQUESTS_SIGNED, "false"));
    }

    /**
     * @return whether the SP requires the assertions to be signed
     * @since 2023.44
     */
    public boolean isSPWantAssertionsSigned() {
        return Boolean.parseBoolean(parameters.get(PARAMETER_WANT_ASSERTIONS_SIGNED))
                || Boolean.parseBoolean(Framework.getProperty(WANT_ASSERTIONS_SIGNED, "false"));
    }

    /**
     * @since 2023.44
     */
    public Optional<String> getSPDigestAlgorithm() {
        return Optional.ofNullable(parameters.get(PARAMETER_DIGEST_ALGORITHM));
    }

    /**
     * @since 2023.44
     */
    @Nonnull
    public List<String> getSPSignatureAlgorithms() {
        return parameters.entrySet()
                         .stream()
                         .filter(entry -> entry.getKey().startsWith(PARAMETER_SIGNATURE_ALGORITHM))
                         .map(Map.Entry::getValue)
                         .toList();
    }

    /**
     * @since 2023.44
     */
    @Nonnull
    public Duration getSPSkewTime() {
        return Optional.ofNullable(parameters.get(PARAMETER_SKEW_TIME))
                       .map(DurationUtils::parse)
                       .or(() -> Optional.ofNullable(Framework.getProperty(SKEW_TIME_MS))
                                         .map(Integer::parseInt)
                                         .map(Duration::ofMillis))
                       .orElseGet(() -> Duration.ofMillis(DEFAULT_SKEW_TIME_MS));
    }

    /**
     * @since 2023.44
     */
    public Optional<KeyHolder> getSPKeyHolder() {
        if (isDefault()) {
            // fallback to the default KeyHolder configuration if the current SAML authentication provider is the
            // default one for backward compatibility
            String keyHolderName = parameters.getOrDefault(PARAMETER_KEY_HOLDER_NAME, DEFAULT_NAME);
            return Framework.getService(KeyManager.class).getKeyHolder(keyHolderName);
        } else if (parameters.containsKey(PARAMETER_KEY_HOLDER_NAME)) {
            return Optional.of(
                    Framework.getService(KeyManager.class)
                             .getKeyHolder(parameters.get(PARAMETER_KEY_HOLDER_NAME))
                             .orElseThrow(() -> new IllegalStateException(
                                     "The KeyHolder referenced by the SAML plugin: " + this + " does not exist")));
        } else {
            return Optional.empty();
        }
    }

    /**
     * @since 2023.44
     */
    @Nonnull
    public EntityDescriptor createSPEntityDescriptor(String baseURL) {
        // Entity Descriptor
        EntityDescriptor descriptor = buildSAMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        // descriptor.setID(id);
        descriptor.setEntityID(getSPEntityId());

        // SPSSO Descriptor
        descriptor.getRoleDescriptors().add(createSPSSODescriptor(baseURL));

        return descriptor;
    }

    /**
     * @since 2023.44
     */
    protected SPSSODescriptor createSPSSODescriptor(String baseURL) {
        SPSSODescriptor spDescriptor = buildSAMLObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        spDescriptor.setAuthnRequestsSigned(isSPAuthnRequestsSigned());
        spDescriptor.setWantAssertionsSigned(isSPWantAssertionsSigned());
        spDescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);

        // Name ID
        spDescriptor.getNameIDFormats().addAll(buildNameIDFormats(nameID));

        // Generate key info
        var keyHolder = getSPKeyHolder();
        keyHolder.flatMap(KeyHolder::getSigningCredential)
                 .map(credential -> buildKeyDescriptor(UsageType.SIGNING, generateKeyInfoForCredential(credential)))
                 .ifPresent(spDescriptor.getKeyDescriptors()::add);
        keyHolder.flatMap(KeyHolder::getEncryptionCredential)
                 .map(credential -> buildKeyDescriptor(UsageType.ENCRYPTION, generateKeyInfoForCredential(credential)))
                 .ifPresent(spDescriptor.getKeyDescriptors()::add);
        keyHolder.flatMap(KeyHolder::getTlsCredential)
                 .map(credential -> buildKeyDescriptor(UsageType.UNSPECIFIED, generateKeyInfoForCredential(credential)))
                 .ifPresent(spDescriptor.getKeyDescriptors()::add);

        // LOGIN
        int index = 0;
        for (String binding : getSPLoginBindings()) {
            AssertionConsumerService consumer = buildSAMLObject(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
            consumer.setLocation(baseURL);
            consumer.setBinding(binding);
            consumer.setIsDefault(index == 0);
            consumer.setIndex(index++);
            spDescriptor.getAssertionConsumerServices().add(consumer);
        }

        // LOGOUT - SAML2_POST_BINDING_URI
        SingleLogoutService logoutService = buildSAMLObject(SingleLogoutService.DEFAULT_ELEMENT_NAME);
        logoutService.setLocation(baseURL);
        logoutService.setBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        spDescriptor.getSingleLogoutServices().add(logoutService);
        return spDescriptor;
    }

    protected List<String> getSPLoginBindings() {
        Set<String> supportedBindings = Stream.of(SAMLInboundBinding.values())
                                              .map(SAMLInboundBinding::getBindingURI)
                                              .collect(toSet());
        List<String> bindings = new ArrayList<>();
        String[] suffixes = parameters.getOrDefault(PARAMETER_LOGIN_BINDINGS,
                Framework.getProperty(LOGIN_BINDINGS, DEFAULT_LOGIN_BINDINGS)).split(",");
        for (String suffix : suffixes) {
            String binding = BINDING_PREFIX + ":" + suffix;
            if (supportedBindings.contains(binding)) {
                bindings.add(binding);
            } else {
                log.warn("Unknown SAML binding: {}", binding);
            }
        }
        return bindings;
    }

    /**
     * @return the Idp metadata URI, it could either be an HTTP URL or a filesystem location
     * @since 2023.44
     */
    public String getIdPMetadataUri() {
        return parameters.get("metadata");
    }

    /**
     * @return the timeout to use when fetching the IdP metadata
     * @since 2023.44
     */
    @Nonnull
    public Duration getIdPMetadataTimeout() {
        return Duration.ofSeconds(Integer.parseInt(parameters.getOrDefault("timeout", "5")));
    }

    public boolean isIdPSignatureMandatory() {
        return Boolean.parseBoolean(parameters.getOrDefault(PARAMETER_SIGNATURE_MANDATORY, "true"));
    }

    /**
     * @since 2023.44
     */
    @Nonnull
    public UserResolver instantiateUserResolver() {
        String userResolverClassName = parameters.get(PARAMETER_USER_RESOLVER_CLASS);
        Class<? extends UserResolver> userResolverClass;
        if (isBlank(userResolverClassName)) {
            UserMapperService ums = Framework.getService(UserMapperService.class);
            if (ums != null) {
                userResolverClass = USERMAPPER_USER_RESOLVER_CLASS;
            } else {
                userResolverClass = DEFAULT_USER_RESOLVER_CLASS;
            }
        } else {
            try {
                userResolverClass = Class.forName(userResolverClassName).asSubclass(AbstractUserResolver.class);
            } catch (ClassNotFoundException | ClassCastException e) {
                throw new NuxeoException("Failed to get user resolver class: " + userResolverClassName, e);
            }

        }
        try {
            var userResolver = userResolverClass.getConstructor().newInstance();
            userResolver.init(parameters);
            return userResolver;
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Failed to initialize user resolver: " + userResolverClassName, e);
        }
    }

    /**
     * @since 2023.44
     */
    public boolean isLoginScreenButtonEnabled() {
        return isNotBlank(parameters.get(PARAMETER_LOGIN_SCREEN_NAME));
    }

    /**
     * @since 2023.44
     */
    public String getLoginScreenName() {
        return parameters.get(PARAMETER_LOGIN_SCREEN_NAME);
    }

    /**
     * @since 2023.44
     */
    public String getLoginScreenDescription() {
        return parameters.get(PARAMETER_LOGIN_SCREEN_DESCRIPTION);
    }

    /**
     * @since 2023.44
     */
    public String getLoginScreenIcon() {
        return parameters.get(PARAMETER_LOGIN_SCREEN_ICON);
    }

    /**
     * @since 2023.44
     */
    public String getLoginScreenLabel() {
        return parameters.get(PARAMETER_LOGIN_SCREEN_LABEL);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                                        // in fact "name" is voluntary not declared to remove the login page button
                                        .append("name", parameters.get(PARAMETER_LOGIN_SCREEN_NAME))
                                        .append("entityId", parameters.get(PARAMETER_ENTITY_ID))
                                        .toString();
    }

    /**
     * @deprecated since 2023.44, use {@link #getSPEntityId()} instead
     */
    @Deprecated(since = "2023.44", forRemoval = true)
    public static String getEntityId() {
        return Framework.getProperty(ENTITY_ID, Framework.getProperty("nuxeo.url"));
    }

    /**
     * @deprecated since 2023.44, use {@link #getSPLoginBindings()} instead
     */
    @Deprecated(since = "2023.44", forRemoval = true)
    public static List<String> getLoginBindings() {
        return retrieveDefaultPluginConfiguration().getSPLoginBindings();
    }

    /**
     * @deprecated since 2023.44, use {@link #isSPAuthnRequestsSigned()} instead
     */
    @Deprecated(since = "2023.44", forRemoval = true)
    public static boolean getAuthnRequestsSigned() {
        return Boolean.parseBoolean(Framework.getProperty(AUTHN_REQUESTS_SIGNED));
    }

    /**
     * @deprecated since 2023.44, use {@link #isSPWantAssertionsSigned()} instead
     */
    @Deprecated(since = "2023.44", forRemoval = true)
    public static boolean getWantAssertionsSigned() {
        return Boolean.parseBoolean(Framework.getProperty(WANT_ASSERTIONS_SIGNED));
    }

    /**
     * @deprecated since 2023.44, use {@link #getSPSkewTime()} instead
     */
    @Deprecated(since = "2023.44", forRemoval = true)
    public static int getSkewTimeMillis() {
        String skewTimeMs = Framework.getProperty(SKEW_TIME_MS);
        return skewTimeMs != null ? Integer.parseInt(skewTimeMs) : DEFAULT_SKEW_TIME_MS;
    }

    /**
     * Returns the {@link EntityDescriptor} for the Nuxeo Service Provider
     * 
     * @deprecated since 2023.44, use {@link #createSPEntityDescriptor} instead
     */
    @Deprecated(since = "2023.44", forRemoval = true)
    public static EntityDescriptor getEntityDescriptor(String baseURL) {
        return retrieveDefaultPluginConfiguration().createSPEntityDescriptor(baseURL);
    }

    /**
     * Returns the {@link SPSSODescriptor} for the Nuxeo Service Provider
     * 
     * @deprecated since 2023.44, use {@link #createSPSSODescriptor} instead
     */
    @Deprecated(since = "2023.44", forRemoval = true)
    public static SPSSODescriptor getSPSSODescriptor(String baseURL) {
        return retrieveDefaultPluginConfiguration().createSPSSODescriptor(baseURL);
    }

    private static KeyDescriptor buildKeyDescriptor(UsageType type, KeyInfo key) {
        KeyDescriptor descriptor = buildSAMLObject(KeyDescriptor.DEFAULT_ELEMENT_NAME);
        descriptor.setUse(type);
        descriptor.setKeyInfo(key);
        return descriptor;
    }

    private static Collection<NameIDFormat> buildNameIDFormats(Collection<String> nameIDs) {

        Collection<NameIDFormat> formats = new LinkedList<>();

        // Populate nameIDs
        for (String nameIDValue : nameIDs) {
            NameIDFormat nameID = buildSAMLObject(NameIDFormat.DEFAULT_ELEMENT_NAME);
            nameID.setURI(nameIDValue);
            formats.add(nameID);
        }

        return formats;
    }

    private static KeyInfo generateKeyInfoForCredential(Credential credential) {
        try {
            KeyInfoGenerator keyInfoGenerator = DefaultSecurityConfigurationBootstrap.buildBasicKeyInfoGeneratorManager()
                                                                                     .getDefaultManager()
                                                                                     .getFactory(credential)
                                                                                     .newInstance();
            return keyInfoGenerator.generate(credential);
        } catch (SecurityException e) {
            log.error("Failed to  generate key info.");
        }
        return null;
    }

    /**
     * Retrieves the {@link SAMLConfiguration} for the default contributed SAML plugin.
     * <p>
     * The plugin defines as the default is:
     * <ul>
     * <li>the one not declaring {@link #PARAMETER_ENTITY_ID entityId} parameter
     * <li>the one having the {@link #PARAMETER_ENTITY_ID entityId} parameter equals to the {@code nuxeo.conf} parameter
     * </ul>
     *
     * @return the {@link SAMLConfiguration} for the default contributed SAML plugin
     * @since 2023.44
     */
    public static SAMLConfiguration retrieveDefaultPluginConfiguration() {
        return Framework.getService(PluggableAuthenticationService.class)
                        .getAuthenticatorPlugins()
                        .stream()
                        .filter(SAMLAuthenticationProvider.class::isInstance)
                        .map(SAMLAuthenticationProvider.class::cast)
                        .map(SAMLAuthenticationProvider::getConfiguration)
                        .filter(SAMLConfiguration::isDefault)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No default SAML Plugin found, check your configuration"));
    }
}
