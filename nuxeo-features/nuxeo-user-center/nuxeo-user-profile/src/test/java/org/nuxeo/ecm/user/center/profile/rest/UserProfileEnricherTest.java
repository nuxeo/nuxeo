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

import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_AVATAR_FIELD;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_BIRTHDATE_FIELD;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_PHONENUMBER_FIELD;
import static org.nuxeo.ecm.user.center.profile.rest.UserProfileEnricher.NAME;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.time.FastDateFormat;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.io.NuxeoPrincipalJsonWriter;
import org.nuxeo.ecm.user.center.profile.UserProfileService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 8.1
 */
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.platform.userworkspace.types")
@Deploy("org.nuxeo.ecm.platform.userworkspace.api")
@Deploy("org.nuxeo.ecm.platform.userworkspace.core")
@Deploy("org.nuxeo.ecm.user.center.profile")
public class UserProfileEnricherTest extends AbstractJsonWriterTest.External<NuxeoPrincipalJsonWriter, NuxeoPrincipal> {

    private static final FastDateFormat FORMATTER = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public UserProfileEnricherTest() {
        super(NuxeoPrincipalJsonWriter.class, NuxeoPrincipal.class);
    }

    @Inject
    CoreSession session;

    @Inject
    protected UserProfileService userProfileService;

    @Inject
    protected DownloadService downloadService;

    private Calendar birthDate;

    protected String avatarURL;

    @Before
    public void setUp() throws IOException {
        birthDate = Calendar.getInstance();
        birthDate.set(1973, 4, 19, 22, 13, 0);
        DocumentModel up = userProfileService.getUserProfileDocument(session);
        up.setPropertyValue(USER_PROFILE_PHONENUMBER_FIELD, "mynumber");
        up.setPropertyValue(USER_PROFILE_BIRTHDATE_FIELD, birthDate.getTime());

        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("data/SmallAvatar.jpg"));
        up.setPropertyValue(USER_PROFILE_AVATAR_FIELD, (Serializable) blob);

        session.saveDocument(up);
        session.save();

        avatarURL = RenderingContext.DEFAULT_URL
                + downloadService.getDownloadUrl(up, USER_PROFILE_AVATAR_FIELD, blob.getFilename());
    }

    @Test
    public void testPrincipal() throws IOException {
        RenderingContext ctx = CtxBuilder.session(session).enrich("user", NAME).get();
        JsonAssert jsonAssert = jsonAssert(session.getPrincipal(), ctx);
        jsonAssert = jsonAssert.get(String.format("contextParameters.%s", NAME));
        jsonAssert.properties(5);
        JsonAssert avatar = jsonAssert.has("avatar").isObject();
        avatar.has("name").isEquals("SmallAvatar.jpg");
        avatar.has("data").isEquals(avatarURL);
        jsonAssert.has("birthdate").isText();
        jsonAssert.has("gender").isFalse();
        jsonAssert.has("locale").isNull();
        jsonAssert.has("phonenumber").isEquals("mynumber");
    }

    /**
     * @since 9.3
     */
    @Test
    public void testPrincipalWithoutDomain() throws IOException {
        List<DocumentRef> refs = session.getChildren(session.getRootDocument().getRef())
                                        .stream()
                                        .map(DocumentModel::getRef)
                                        .collect(Collectors.toList());
        session.removeDocuments(refs.toArray(new DocumentRef[refs.size()]));
        session.save();
        RenderingContext ctx = CtxBuilder.session(session).enrich("user", NAME).get();
        JsonAssert jsonAssert = jsonAssert(session.getPrincipal(), ctx);
        jsonAssert = jsonAssert.get(String.format("contextParameters.%s", NAME)).isNull();
    }

    @Test
    @Deploy("org.nuxeo.ecm.user.center.profile:OSGI-INF/test-profile-enricher-compat-contrib.xml")
    public void testCompatibility() throws IOException {
        RenderingContext ctx = CtxBuilder.session(session).enrich("user", NAME).get();
        JsonAssert jsonAssert = jsonAssert(session.getPrincipal(), ctx);
        jsonAssert = jsonAssert.get(String.format("contextParameters.%s", NAME));
        jsonAssert.properties(3);
        jsonAssert.has("avatar").isEquals(avatarURL);
        jsonAssert.has("birthdate");
        jsonAssert.get("phonenumber").isEquals("mynumber");
        jsonAssert.get("birthdate").isEquals(FORMATTER.format(birthDate));
    }
}
