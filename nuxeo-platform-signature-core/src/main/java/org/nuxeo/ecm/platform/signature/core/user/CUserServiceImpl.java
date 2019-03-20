/*
 * (C) Copyright 2011-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    Wojciech Sulejman
 */
package org.nuxeo.ecm.platform.signature.core.user;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.pki.RootService;
import org.nuxeo.ecm.platform.signature.api.user.AliasType;
import org.nuxeo.ecm.platform.signature.api.user.AliasWrapper;
import org.nuxeo.ecm.platform.signature.api.user.CNField;
import org.nuxeo.ecm.platform.signature.api.user.CUserService;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Base implementation of the user certificate service.
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */
public class CUserServiceImpl extends DefaultComponent implements CUserService {

    private static final Log LOG = LogFactory.getLog(CUserServiceImpl.class);

    private static final String CERTIFICATE_DIRECTORY_NAME = "certificate";

    /**
     * Configurable country code
     */
    protected String countryCode;

    /**
     * Configurable organization name
     */
    protected String organization;

    /**
     * Configurable organizational unit name
     */
    protected String organizationalUnit;

    @Override
    public UserInfo getUserInfo(DocumentModel userModel) throws CertException {
        UserInfo userInfo;
        String userID = (String) userModel.getPropertyValue("user:username");
        String firstName = (String) userModel.getPropertyValue("user:firstName");
        String lastName = (String) userModel.getPropertyValue("user:lastName");
        String email = (String) userModel.getPropertyValue("user:email");

        Map<CNField, String> userFields = new HashMap<>();

        userFields.put(CNField.C, countryCode);
        userFields.put(CNField.O, organization);
        userFields.put(CNField.OU, organizationalUnit);

        userFields.put(CNField.CN, firstName + " " + lastName);
        userFields.put(CNField.Email, email);
        userFields.put(CNField.UserID, userID);
        userInfo = new UserInfo(userFields);
        return userInfo;
    }

    @Override
    public KeyStore getUserKeystore(String userID, String userKeystorePassword) throws CertException {
        String keystore64Encoded = Framework.doPrivileged(() -> {
            try (Session session = getDirectoryService().open(CERTIFICATE_DIRECTORY_NAME)) {
                DocumentModel entry = session.getEntry(userID);
                if (entry != null) {
                    return (String) entry.getPropertyValue("cert:keystore");
                } else {
                    throw new CertException("No directory entry for " + userID);
                }
            }
        });
        byte[] keystoreBytes = Base64.decodeBase64(keystore64Encoded);
        ByteArrayInputStream byteIS = new ByteArrayInputStream(keystoreBytes);
        return getCertService().getKeyStore(byteIS, userKeystorePassword);
    }

    @Override
    public DocumentModel createCertificate(DocumentModel user, String userKeyPassword) throws CertException {
        return Framework.doPrivileged(() -> {
            try (Session session = getDirectoryService().open(CERTIFICATE_DIRECTORY_NAME)) {
                DocumentModel certificate = null;

                // create an entry in the directory
                String userID = (String) user.getPropertyValue("user:username");

                // make sure that no certificates are associated with the
                // current userid
                boolean certificateExists = session.hasEntry(userID);
                if (certificateExists) {
                    throw new CertException(userID + " already has a certificate");
                }

                LOG.info("Starting certificate generation for: " + userID);
                Map<String, Object> map = new HashMap<>();
                map.put("userid", userID);

                // add a keystore to a directory entry
                KeyStore keystore = getCertService().initializeUser(getUserInfo(user), userKeyPassword);
                ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
                getCertService().storeCertificate(keystore, byteOS, userKeyPassword);
                String keystore64Encoded = Base64.encodeBase64String(byteOS.toByteArray());
                map.put("keystore", keystore64Encoded);
                map.put("certificate", getUserCertInfo(keystore, user));
                map.put("keypassword", userKeyPassword);
                certificate = session.createEntry(map);
                return certificate;
            } catch (DirectoryException e) {
                LOG.error(e);
                throw new CertException(e);
            }
        });
    }

    protected static DirectoryService getDirectoryService() {
        return Framework.getService(DirectoryService.class);
    }

    @Override
    public String getUserCertInfo(DocumentModel user, String userKeyPassword) throws CertException {
        String userID = (String) user.getPropertyValue("user:username");
        KeyStore keystore = getUserKeystore(userID, userKeyPassword);
        return getUserCertInfo(keystore, user);
    }

    private String getUserCertInfo(KeyStore keystore, DocumentModel user) throws CertException {
        String userCertInfo = null;
        if (null != keystore) {
            String userID = (String) user.getPropertyValue("user:username");
            AliasWrapper alias = new AliasWrapper(userID);
            X509Certificate certificate = getCertService().getCertificate(keystore, alias.getId(AliasType.CERT));
            userCertInfo = certificate.getSubjectDN() + " valid till: " + certificate.getNotAfter();
        }
        return userCertInfo;
    }

    @Override
    public DocumentModel getCertificate(String userID) {
        return Framework.doPrivileged(() -> {
            try (Session session = getDirectoryService().open(CERTIFICATE_DIRECTORY_NAME)) {
                DocumentModel certificate = session.getEntry(userID);
                return certificate;
            }
        });
    }

    @Override
    public byte[] getRootCertificateData() {
        byte[] certificateData = getRootService().getRootPublicCertificate();
        return certificateData;
    }

    @SuppressWarnings("boxing")
    @Override
    public boolean hasCertificate(String userID) throws CertException {
        return Framework.doPrivileged(() -> {
            try (Session session = getDirectoryService().open(CERTIFICATE_DIRECTORY_NAME)) {
                return session.getEntry(userID) != null;
            }
        });
    }

    @Override
    public void deleteCertificate(String userID) throws CertException {
        Framework.doPrivileged(() -> {
            try (Session session = getDirectoryService().open(CERTIFICATE_DIRECTORY_NAME)) {
                DocumentModel certEntry = session.getEntry(userID);
                session.deleteEntry(certEntry);
                assert (null == session.getEntry(userID));
            }
        });
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof CUserDescriptor) {
            CUserDescriptor desc = (CUserDescriptor) contribution;
            countryCode = desc.getCountryCode();
            organization = desc.getOrganization();
            organizationalUnit = desc.getOrganizationalUnit();
        }
    }

    protected CertService getCertService() {
        return Framework.getService(CertService.class);
    }

    protected RootService getRootService() {
        return Framework.getService(RootService.class);
    }
}
