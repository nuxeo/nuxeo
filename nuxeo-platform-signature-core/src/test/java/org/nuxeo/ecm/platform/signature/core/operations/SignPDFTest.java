/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 *      Mickael Vachette <mv@nuxeo.com>
 */
package org.nuxeo.ecm.platform.signature.core.operations;

import com.google.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
import org.nuxeo.ecm.platform.signature.api.user.CUserService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, AutomationFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.signature.core", "org.nuxeo.ecm.platform" + ".signature.core.test" })
public class SignPDFTest {

    protected static final String ORIGINAL_PDF = "pdf-tests/original.pdf";

    protected static final String USER_KEY_PASSWORD = "abc";

    protected static final String CERTIFICATE_DIRECTORY_NAME = "certificate";

    protected static final String DEFAULT_USER_ID = "hsimpsons";

    @Inject
    protected CUserService cUserService;

    @Inject
    protected SignatureService signatureService;

    @Inject
    protected UserManager userManager;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    CoreSession session;

    @Inject
    AutomationService automationService;

    protected File origPdfFile;

    protected DocumentModel user;

    /**
     * Signing Prerequisite: a user with a certificate needs to be present
     */
    @Before
    public void setUp() throws Exception {
        DocumentModel userModel = userManager.getBareUserModel();
        userModel.setProperty("user", "username", DEFAULT_USER_ID);
        userModel.setProperty("user", "firstName", "Homer");
        userModel.setProperty("user", "lastName", "Simpson");
        userModel.setProperty("user", "email", "hsimpson@springfield.com");
        userModel.setPathInfo("/", DEFAULT_USER_ID);
        user = userManager.createUser(userModel);
        DocumentModel certificate = cUserService.createCertificate(user, USER_KEY_PASSWORD);
        assertNotNull(certificate);
        origPdfFile = FileUtils.getResourceFileFromContext(ORIGINAL_PDF);
    }

    @After
    public void tearDown() throws Exception {
        // delete certificates associated with user ids
        Session sqlSession = directoryService.open(CERTIFICATE_DIRECTORY_NAME);
        sqlSession.deleteEntry(DEFAULT_USER_ID);
        sqlSession.close();
        // delete users
        userManager.deleteUser(DEFAULT_USER_ID);
    }

    @Test
    public void testSignPDF() throws Exception {
        // first user signs
        Blob origBlob = Blobs.createBlob(origPdfFile);
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(origBlob);
        Map<String, Object> params = new HashMap<>();
        params.put("username", DEFAULT_USER_ID);
        params.put("password", USER_KEY_PASSWORD);
        params.put("reason", "TEST");
        Blob signedBlob = (Blob) automationService.run(ctx, SignPDF.ID, params);
        assertNotNull(signedBlob);
    }
}