/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 *      Mickael Vachette <mv@nuxeo.com>
 *      Estelle Giuly <egiuly@nuxeo.com>
 */
package org.nuxeo.ecm.platform.signature.core.operations;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.signature.api.user.CUserService;
import org.nuxeo.ecm.platform.signature.core.SignatureCoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ SignatureCoreFeature.class, PlatformFeature.class, AutomationFeature.class })
public class SignPDFTest {

    protected static final String ORIGINAL_PDF = "pdf-tests/original.pdf";

    protected static final String USER_KEY_PASSWORD = "abc";

    protected static final String CERTIFICATE_DIRECTORY_NAME = "certificate";

    protected static final String DEFAULT_USER_ID = "hsimpsons";

    @Inject
    protected CUserService cUserService;

    @Inject
    protected UserManager userManager;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    protected File origPdfFile;

    protected DocumentModel user;

    /**
     * Signing Prerequisite: a user with a certificate needs to be present
     */
    @Before
    public void setUp() {
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
    public void tearDown() {
        // delete certificates associated with user ids
        try (Session sqlSession = directoryService.open(CERTIFICATE_DIRECTORY_NAME)) {
            sqlSession.deleteEntry(DEFAULT_USER_ID);
        }
        // delete users
        userManager.deleteUser(DEFAULT_USER_ID);
    }

    @Test
    public void testSignPDF() throws Exception {
        // first user signs
        OperationContext ctx = buildCtx(session);
        Map<String, Object> params = buildParams();
        Blob signedBlob = (Blob) automationService.run(ctx, SignPDF.ID, params);
        assertNotNull(signedBlob);
    }

    @Test
    public void testNotAllowedToSignPDF() throws Exception {
        try (CloseableCoreSession notAdminSession = CoreInstance.openCoreSession(session.getRepositoryName(),
                DEFAULT_USER_ID)) {
            OperationContext ctx = buildCtx(notAdminSession);
            Map<String, Object> params = buildParams();
            try {
                automationService.run(ctx, SignPDF.ID, params);
            } catch (OperationException e) {
                assertNotNull(e.getMessage());
                assertTrue(e.getMessage().contains("Not allowed"));
            }
        }
    }

    protected OperationContext buildCtx(CoreSession coreSession) throws IOException {
        OperationContext ctx = new OperationContext(coreSession);
        Blob origBlob = Blobs.createBlob(origPdfFile);
        ctx.setInput(origBlob);
        return ctx;
    }

    protected Map<String, Object> buildParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("username", DEFAULT_USER_ID);
        params.put("password", USER_KEY_PASSWORD);
        params.put("reason", "TEST");
        return params;
    }
}
