/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Wojciech Sulejman
 */

package org.nuxeo.ecm.platform.signature.core.pki;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.hsqldb.jdbcDriver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.jndi.NamingContextFactory;
import org.nuxeo.common.utils.Base64;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryServiceImpl;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.sql.SQLDirectoryProxy;
import org.nuxeo.ecm.directory.sql.SimpleDataSource;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.pki.RootService;
import org.nuxeo.ecm.platform.signature.api.user.AliasType;
import org.nuxeo.ecm.platform.signature.api.user.AliasWrapper;
import org.nuxeo.ecm.platform.signature.api.user.CNField;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 * 
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(type = BackendType.H2, user = "Administrator", init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy( { "org.nuxeo.ecm.core", "org.nuxeo.ecm.core.api",
        "org.nuxeo.runtime.management", "org.nuxeo.ecm.directory",
        "org.nuxeo.common", "org.nuxeo.ecm.directory.sql",
        "org.nuxeo.ecm.platform.signature.core",
        "org.nuxeo.ecm.platform.signature.core.test" })
public class TypeTest {

    DocumentModel doc;

    @Inject
    protected CoreSession session;

    @Inject
    protected CertService certService;

    private static final String ROOT_KEY_PASSWORD = "abc";

    private static final String ROOT_KEYSTORE_PASSWORD = "abc";

    private static final String ROOT_USER_ID = "PDFCA";

    private static final String USER_KEY_PASSWORD = "abc";

    private static final String USER_KEYSTORE_PASSWORD = "abc";

    protected String keystorePath = "test-files/keystore.jks";

    @Before
    public void setup() throws Exception {

        NamingContextFactory.setAsInitial();
        setUpContextFactory();

        KeyStore rootKeystore = certService.getKeyStore(
                getKeystoreIS(keystorePath), ROOT_KEYSTORE_PASSWORD);
        RootService rootService = new RootServiceImpl();
        AliasWrapper alias = new AliasWrapper(ROOT_USER_ID);
        rootService.setRootKeyAlias(alias.getId(AliasType.KEY));
        rootService.setRootCertificateAlias(alias.getId(AliasType.CERT));
        rootService.setRootKeyPassword(ROOT_KEY_PASSWORD);
        rootService.setRootKeyStore(rootKeystore);
        rootService.setRootKeystorePassword(ROOT_KEYSTORE_PASSWORD);
        certService.setRootService(rootService);
    }

    @Test
    public void savePropertiesToDirectory() throws Exception {
        // create an entry in the directory
        String userID = "testUserID2";
        Session sqlSession = getDirectoryService().open("certificate");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("userid", userID);

        // add a keystore to the entry
        KeyStore keystore = generateUserKeystore(userID);
        ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
        keystore.store(byteOS, USER_KEYSTORE_PASSWORD.toCharArray());
        String keystore64Encoded = Base64.encodeBytes(byteOS.toByteArray());
        map.put("keystore", keystore64Encoded);

        DocumentModel entry = sqlSession.createEntry(map);
        assertNotNull(entry);
        sqlSession.commit();

        // retrieve a persisted entry from the directory
        DocumentModel entryFromSession = sqlSession.getEntry(userID);
        String keystore64EncodedFromSession = (String) entryFromSession.getPropertyValue("cert:keystore");
        byte[] keystoreBytes = Base64.decode(keystore64EncodedFromSession);
        ByteArrayInputStream keystoreByteIS = new ByteArrayInputStream(
                keystoreBytes);
        keystore.load(keystoreByteIS, USER_KEYSTORE_PASSWORD.toCharArray());
        AliasWrapper userAlias = new AliasWrapper(userID);

        // check if you can read an existing alias from the persisted keystore
        assertTrue(keystore.containsAlias(userAlias.getId(AliasType.KEY)));

        sqlSession.deleteEntry(userID);
        sqlSession.commit();
        sqlSession.close();
    }

    public KeyStore generateUserKeystore(String userID) throws Exception {
        KeyStore keystore = certService.initializeUser(getUserInfo(userID),
                USER_KEY_PASSWORD);
        return keystore;
    }

    public UserInfo getUserInfo(String userID) throws Exception {
        Map<CNField, String> userFields;
        userFields = new HashMap<CNField, String>();
        userFields.put(CNField.CN, "Wojciech Sulejman");
        userFields.put(CNField.C, "US");
        userFields.put(CNField.OU, "IT");
        userFields.put(CNField.O, "Nuxeo");
        userFields.put(CNField.UserID, userID);
        userFields.put(CNField.Email, "wsulejman@nuxeo.com");
        UserInfo userInfo = new UserInfo(userFields);
        return userInfo;
    }

    protected static DirectoryService getDirectoryService()
            throws ClientException {
        try {
            return Framework.getService(DirectoryService.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public UserInfo getPDFCAInfo() throws Exception {
        Map<CNField, String> userFields;
        userFields = new HashMap<CNField, String>();
        userFields.put(CNField.CN, "PDFCA");
        userFields.put(CNField.C, "US");
        userFields.put(CNField.OU, "CA");
        userFields.put(CNField.O, "Nuxeo");
        userFields.put(CNField.UserID, "PDFCA");
        userFields.put(CNField.Email, "pdfca@nuxeo.com");
        UserInfo userInfo = new UserInfo(userFields);
        return userInfo;
    }

    KeyStore getKeystore(String password) throws Exception {
        KeyStore keystore = certService.getKeyStore(
                getKeystoreIS(keystorePath), password);
        return keystore;
    }

    InputStream getKeystoreIS(String keystoreFilePath) throws Exception {
        File keystoreFile = FileUtils.getResourceFileFromContext(keystoreFilePath);
        return new FileInputStream(keystoreFile);
    }

    protected static Directory getDirectory(String dirName)
            throws DirectoryException {
        DirectoryServiceImpl dirServiceImpl = (DirectoryServiceImpl) Framework.getRuntime().getComponent(
                DirectoryService.NAME);
        Directory dir = dirServiceImpl.getDirectory(dirName);
        if (dir instanceof SQLDirectoryProxy) {
            dir = ((SQLDirectoryProxy) dir).getDirectory();
        }
        return dir;
    }

    public static void setUpContextFactory() throws NamingException {
        Context context = new InitialContext();
        DataSource datasource = new SimpleDataSource("jdbc:hsqldb:mem:memid",
                jdbcDriver.class.getName(), "SA", "");
        DataSource datasourceAutocommit = new SimpleDataSource(
                "jdbc:hsqldb:mem:memid", jdbcDriver.class.getName(), "SA", "") {
            @Override
            public Connection getConnection() throws SQLException {
                Connection con = super.getConnection();
                con.setAutoCommit(true);
                return con;
            }
        };
        assertNotNull(datasourceAutocommit);
        context.bind("java:comp/env/jdbc/nxsqldirectory", datasource);
    }
}