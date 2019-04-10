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

package org.nuxeo.ecm.platform.signature.core.user;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.hsqldb.jdbcDriver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.jndi.NamingContextFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.sql.SimpleDataSource;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.user.CUserService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
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
@Features(CoreFeature.class)
@RepositoryConfig(cleanup=Granularity.METHOD, type = BackendType.H2, user = "Administrator")
@Deploy( { "org.nuxeo.ecm.core", "org.nuxeo.ecm.core.api",
        "org.nuxeo.runtime.management", "org.nuxeo.ecm.directory.api",
         "org.nuxeo.ecm.directory",
        "org.nuxeo.ecm.directory.sql", "org.nuxeo.ecm.platform.usermanager",
        "org.nuxeo.ecm.platform.usermanager.api",
        "org.nuxeo.ecm.platform.signature.core",
        "org.nuxeo.ecm.platform.signature.core.test" })
public class CUserServiceTest {

    private static final String USER_KEYSTORE_PASSWORD = "abc";

    @Inject
    protected CUserService cUserService;

    @Inject
    protected CertService certService;

    protected DocumentModel user;

    private static final String USER_ID = "hsimpson";

    @Before
    public void setup() throws Exception {
        setUpContextFactory();
    }

    public void testCreateCert() throws Exception {
        DocumentModel certificate = cUserService.createCertificate(getUser(),
                USER_KEYSTORE_PASSWORD);
        assertTrue(certificate.getPropertyValue("cert:userid").equals(USER_ID));
    }

    @Test
    public void testGetCertificate() throws Exception {
        // try to retrieve a certificate that does not yet exist
        DocumentModel retrievedCertificate = cUserService.getCertificate(USER_ID);
        assertNull(retrievedCertificate);
        // add missing certificate
        DocumentModel createdCertificate = cUserService.createCertificate(getUser(),
                USER_KEYSTORE_PASSWORD);
        assertNotNull(createdCertificate);
        // retry
        retrievedCertificate = cUserService.getCertificate(USER_ID);
        assertNotNull("The certificate could not be retrieved from the directory",retrievedCertificate);
        assertTrue(retrievedCertificate.getPropertyValue("cert:userid").equals(USER_ID));
    }

    public DocumentModel getUser() throws Exception {
        if (user == null) {
            user = getUserManager().getUserModel(USER_ID);
            if (user == null) {
                DocumentModel userModel = getUserManager().getBareUserModel();
                userModel.setProperty("user", "username", USER_ID);
                userModel.setProperty("user", "firstName", "Homer");
                userModel.setProperty("user", "lastName", "Simpson");
                userModel.setProperty("user", "email", "simps@on.com");
                userModel.setPathInfo("/", USER_ID);
                user = getUserManager().createUser(userModel);
            }
        }
        return user;
    }

    protected static UserManager getUserManager() {
        UserManager userManager = Framework.getLocalService(UserManager.class);
        assertNotNull(userManager);
        return userManager;
    }

    public static void setUpContextFactory() throws NamingException {
        NamingContextFactory.setAsInitial();
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