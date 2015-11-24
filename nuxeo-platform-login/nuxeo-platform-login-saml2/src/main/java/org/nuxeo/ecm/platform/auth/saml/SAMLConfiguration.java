/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *      Nelson Silva
 */

package org.nuxeo.ecm.platform.auth.saml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.auth.saml.key.KeyManager;
import org.nuxeo.runtime.api.Framework;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.NameIDType;
import org.opensaml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml2.metadata.NameIDFormat;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml2.metadata.SingleLogoutService;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.keyinfo.KeyInfoGenerator;
import org.opensaml.xml.signature.KeyInfo;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

/**
 * @since 7.3
 */
public class SAMLConfiguration {

    protected static final Log log = LogFactory.getLog(SAMLConfiguration.class);

    public static final String ENTITY_ID = "nuxeo.saml2.entityId";

    public static final String AUTHN_REQUESTS_SIGNED = "nuxeo.saml2.authnRequestsSigned";

    public static final String WANT_ASSERTIONS_SIGNED = "nuxeo.saml2.wantAssertionsSigned";

    public static final Collection<String> nameID = Arrays.asList(NameIDType.EMAIL, NameIDType.TRANSIENT,
        NameIDType.PERSISTENT, NameIDType.UNSPECIFIED, NameIDType.X509_SUBJECT);

    private SAMLConfiguration() {

    }

    public static String getEntityId() {
        return Framework.getProperty(ENTITY_ID, Framework.getProperty("nuxeo.url"));
    }

    public static boolean getAuthnRequestsSigned() {
        return Boolean.parseBoolean(Framework.getProperty(AUTHN_REQUESTS_SIGNED));
    }

    public static boolean getWantAssertionsSigned() {
        return Boolean.parseBoolean(Framework.getProperty(WANT_ASSERTIONS_SIGNED));
    }

    /**
     * Returns the {@link EntityDescriptor} for the Nuxeo Service Provider
     */
    public static EntityDescriptor getEntityDescriptor(String baseURL) {

        // Entity Descriptor
        EntityDescriptor descriptor = build(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        // descriptor.setID(id);
        descriptor.setEntityID(getEntityId());

        // SPSSO Descriptor
        SPSSODescriptor spDescriptor = build(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        spDescriptor.setAuthnRequestsSigned(getAuthnRequestsSigned());
        spDescriptor.setWantAssertionsSigned(getWantAssertionsSigned());
        spDescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);

        // Name ID
        spDescriptor.getNameIDFormats().addAll(buildNameIDFormats(nameID));

        // Generate key info
        KeyManager keyManager = Framework.getLocalService(KeyManager.class);
        if (keyManager.getSigningCredential() != null) {
            spDescriptor.getKeyDescriptors().add(
                buildKeyDescriptor(UsageType.SIGNING,
                    generateKeyInfoForCredential(keyManager.getSigningCredential())));
        }
        if (keyManager.getEncryptionCredential() != null) {
            spDescriptor.getKeyDescriptors().add(
                buildKeyDescriptor(UsageType.ENCRYPTION,
                    generateKeyInfoForCredential(keyManager.getEncryptionCredential())));
        }
        if (keyManager.getTlsCredential() != null) {
            spDescriptor.getKeyDescriptors().add(
                buildKeyDescriptor(UsageType.UNSPECIFIED,
                    generateKeyInfoForCredential(keyManager.getTlsCredential())));
        }

        // LOGIN - SAML2_REDIRECT_BINDING_URI
        AssertionConsumerService consumer = build(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
        consumer.setLocation(baseURL);
        consumer.setBinding(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
        consumer.setIndex(1);
        consumer.setIsDefault(true);
        spDescriptor.getAssertionConsumerServices().add(consumer);

        // LOGIN - SAML2_POST_BINDING_URI
        consumer = build(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
        consumer.setLocation(baseURL);
        consumer.setBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        consumer.setIndex(2);
        spDescriptor.getAssertionConsumerServices().add(consumer);

        // LOGOUT - SAML2_POST_BINDING_URI
        SingleLogoutService logoutService = build(SingleLogoutService.DEFAULT_ELEMENT_NAME);
        logoutService.setLocation(baseURL);
        logoutService.setBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        spDescriptor.getSingleLogoutServices().add(logoutService);

        descriptor.getRoleDescriptors().add(spDescriptor);

        return descriptor;
    }

    private static KeyDescriptor buildKeyDescriptor(UsageType type, KeyInfo key) {
        KeyDescriptor descriptor = build(KeyDescriptor.DEFAULT_ELEMENT_NAME);
        descriptor.setUse(type);
        descriptor.setKeyInfo(key);
        return descriptor;
    }

    private static Collection<NameIDFormat> buildNameIDFormats(Collection<String> nameIDs) {

        Collection<NameIDFormat> formats = new LinkedList<>();

        // Populate nameIDs
        for (String nameIDValue : nameIDs) {
            NameIDFormat nameID = build(NameIDFormat.DEFAULT_ELEMENT_NAME);
            nameID.setFormat(nameIDValue);
            formats.add(nameID);
        }

        return formats;
    }

    private static KeyInfo generateKeyInfoForCredential(Credential credential) {
        try {
            KeyInfoGenerator keyInfoGenerator = SecurityHelper.getKeyInfoGenerator(credential, null, null);
            return keyInfoGenerator.generate(credential);
        } catch (org.opensaml.xml.security.SecurityException e) {
            log.error("Failed to  generate key info.");
        }
        return null;
    }

    private static <T extends SAMLObject> T build(QName qName) {
        return (T) Configuration.getBuilderFactory().getBuilder(qName).buildObject(qName);
    }

}
