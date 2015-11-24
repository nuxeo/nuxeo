/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */

package org.nuxeo.ecm.platform.auth.saml.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.auth.saml.key.KeyManager;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.NameIDType;
import org.opensaml.saml2.metadata.*;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.keyinfo.KeyInfoGenerator;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Element;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Servlet that return local SP metadata for configuring IdPs.
 *
 * @since 5.9.6
 */
public class MetadataServlet extends HttpServlet {

    public static final Collection<String> nameID = Arrays.asList(
            NameIDType.EMAIL,
            NameIDType.TRANSIENT,
            NameIDType.PERSISTENT,
            NameIDType.UNSPECIFIED,
            NameIDType.X509_SUBJECT);

    protected static final Log log = LogFactory.getLog(MetadataServlet.class);

    protected XMLObjectBuilderFactory builderFactory;

    private KeyManager keyManager;

    private String entityBaseURL;

    private String entityId = "nuxeo";

    private boolean signMetadata = true;

    private boolean requestSigned = true;

    private boolean wantAssertionSigned = true;

    @Override
    public void init() throws ServletException {
        builderFactory = Configuration.getBuilderFactory();
    }

    private KeyManager getKeyManager() {
        if (keyManager == null) {
            keyManager = Framework.getLocalService(KeyManager.class);
        }
        return keyManager;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        entityBaseURL = VirtualHostHelper.getBaseURL(request) + '/' +
                NuxeoAuthenticationFilter.DEFAULT_START_PAGE;
        /*
        id = entityId.replaceAll("[^a-zA-Z0-9-_.]", "_");
        if (id.startsWith("-")) {
            id = "_" + id.substring(1);
        }*/

        EntityDescriptor descriptor = buildEntityDescriptor();

        try {
            Marshaller marshaller = Configuration.getMarshallerFactory().getMarshaller(
                    descriptor);
            if (marshaller == null) {
                log.error(
                        "Unable to marshall message, no marshaller registered for message object: "
                                + descriptor.getElementQName());
            }
            Element dom = marshaller.marshall(descriptor);
            XMLHelper.writeNode(dom, response.getWriter());
        } catch (MarshallingException e) {
            log.error("Unable to write metadata.");
        }
    }

    protected EntityDescriptor buildEntityDescriptor() {

        // Entity Descriptor
        EntityDescriptor descriptor = build(
                EntityDescriptor.DEFAULT_ELEMENT_NAME);
        //descriptor.setID(id);
        descriptor.setEntityID(entityId);

        // SPSSO Descriptor
        SPSSODescriptor spDescriptor = build(
                SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        spDescriptor.setAuthnRequestsSigned(requestSigned);
        spDescriptor.setWantAssertionsSigned(wantAssertionSigned);
        spDescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);

        // Name ID
        spDescriptor.getNameIDFormats().addAll(buildNameIDFormats(nameID));

        // Generate key info
        if (getKeyManager().getSigningCredential() != null) {
            spDescriptor.getKeyDescriptors().add(
                    buildKeyDescriptor(UsageType.SIGNING,
                            generateKeyInfoForCredential(
                                    getKeyManager().getSigningCredential())));
        }
        if (getKeyManager().getEncryptionCredential() != null) {
            spDescriptor.getKeyDescriptors().add(
                    buildKeyDescriptor(UsageType.ENCRYPTION,
                            generateKeyInfoForCredential(
                                    getKeyManager().getEncryptionCredential())));
        }
        if (getKeyManager().getTlsCredential() != null) {
            spDescriptor.getKeyDescriptors().add(
                    buildKeyDescriptor(UsageType.UNSPECIFIED,
                            generateKeyInfoForCredential(
                                    getKeyManager().getTlsCredential())));
        }

        // LOGIN -  SAML2_POST_BINDING_URI
        AssertionConsumerService consumer = build(
                AssertionConsumerService.DEFAULT_ELEMENT_NAME);
        consumer.setLocation(entityBaseURL);
        consumer.setBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        consumer.setIsDefault(true);
        consumer.setIndex(0);
        spDescriptor.getAssertionConsumerServices().add(consumer);

        // LOGOUT - SAML2_POST_BINDING_URI
        SingleLogoutService logoutService = build(
                SingleLogoutService.DEFAULT_ELEMENT_NAME);
        logoutService.setLocation(entityBaseURL);
        logoutService.setBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        spDescriptor.getSingleLogoutServices().add(logoutService);

        descriptor.getRoleDescriptors().add(spDescriptor);

        return descriptor;
    }

    protected KeyDescriptor buildKeyDescriptor(UsageType type, KeyInfo key) {
        KeyDescriptor descriptor = build(KeyDescriptor.DEFAULT_ELEMENT_NAME);
        descriptor.setUse(type);
        descriptor.setKeyInfo(key);
        return descriptor;
    }

    protected Collection<NameIDFormat> buildNameIDFormats(
            Collection<String> nameIDs) {

        Collection<NameIDFormat> formats = new LinkedList<>();

        // Populate nameIDs
        for (String nameIDValue : nameIDs) {
            NameIDFormat nameID = build(NameIDFormat.DEFAULT_ELEMENT_NAME);
            nameID.setFormat(nameIDValue);
            formats.add(nameID);
        }

        return formats;
    }

    protected KeyInfo generateKeyInfoForCredential(Credential credential) {
        try {
            KeyInfoGenerator keyInfoGenerator = SecurityHelper.getKeyInfoGenerator(
                    credential,
                    null,
                    null);
            return keyInfoGenerator.generate(credential);
        } catch (org.opensaml.xml.security.SecurityException e) {
            log.error("Failed to  generate key info.");
        }
        return null;
    }

    protected <T extends SAMLObject> T build(QName qName) {
        return (T) builderFactory.getBuilder(qName).buildObject(qName);
    }
}
