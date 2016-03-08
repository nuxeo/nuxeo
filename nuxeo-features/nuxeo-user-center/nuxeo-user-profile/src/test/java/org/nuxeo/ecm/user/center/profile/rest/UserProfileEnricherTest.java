/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.user.center.profile.rest;

import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_BIRTHDATE_FIELD;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_PHONENUMBER_FIELD;
import static org.nuxeo.ecm.user.center.profile.rest.UserProfileEnricher.NAME;

import java.io.IOException;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.io.NuxeoPrincipalJsonWriter;
import org.nuxeo.ecm.user.center.profile.UserProfileService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 8.1
 */
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.types", "org.nuxeo.ecm.platform.userworkspace.api",
        "org.nuxeo.ecm.platform.userworkspace.core", "org.nuxeo.ecm.user.center.profile" })
public class UserProfileEnricherTest extends AbstractJsonWriterTest.External<NuxeoPrincipalJsonWriter, NuxeoPrincipal> {
    public UserProfileEnricherTest() {
        super(NuxeoPrincipalJsonWriter.class, NuxeoPrincipal.class);
    }

    @Inject
    CoreSession session;

    @Before
    public void setUp() {
        UserProfileService ups = Framework.getLocalService(UserProfileService.class);
        DocumentModel up = ups.getUserProfileDocument(session);
        up.setPropertyValue(USER_PROFILE_PHONENUMBER_FIELD, "mynumber");
        up.setPropertyValue(USER_PROFILE_BIRTHDATE_FIELD, new Date());
        session.saveDocument(up);
        session.save();
    }

    @Test
    public void testPrincipal() throws IOException {
        RenderingContext ctx = CtxBuilder.session(session).enrich("user", NAME).get();
        JsonAssert jsonAssert = jsonAssert((NuxeoPrincipal) session.getPrincipal(), ctx);
        jsonAssert = jsonAssert.get(String.format("contextParameters.%s", NAME));
        jsonAssert.has(String.format("avatar", NAME));
        jsonAssert.has(String.format("birthdate", NAME));
        jsonAssert.get(String.format("phonenumber", NAME)).isEquals("mynumber");
    }
}
