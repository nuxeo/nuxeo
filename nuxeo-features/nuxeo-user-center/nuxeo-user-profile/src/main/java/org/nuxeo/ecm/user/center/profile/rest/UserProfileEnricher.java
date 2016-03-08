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

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_AVATAR_FIELD;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_BIRTHDATE_FIELD;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_PHONENUMBER_FIELD;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.user.center.profile.UserProfileService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 8.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class UserProfileEnricher extends AbstractJsonEnricher<NuxeoPrincipal> {

    private static final DateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static final String NAME = "userprofile";

    public UserProfileEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, NuxeoPrincipal nuxeoPrincipal) throws IOException {

        UserProfileService ups = Framework.getLocalService(UserProfileService.class);
        try (RenderingContext.SessionWrapper sw = ctx.getSession(null)) {
            DocumentModel up = ups.getUserProfileDocument(nuxeoPrincipal.getName(), sw.getSession());

            jg.writeFieldName(NAME);
            if (up == null) {
                jg.writeNull();
            } else {
                try {
                    jg.writeStartObject();

                    Serializable propertyValue = up.getPropertyValue(USER_PROFILE_BIRTHDATE_FIELD);
                    jg.writeStringField(
                            "birthdate",
                            propertyValue == null ? null
                                    : FORMATTER.format(((GregorianCalendar) propertyValue).getTime()));
                    jg.writeStringField("phonenumber", (String) up.getPropertyValue(USER_PROFILE_PHONENUMBER_FIELD));

                    Blob avatar = (Blob) up.getPropertyValue(USER_PROFILE_AVATAR_FIELD);
                    if (avatar != null) {
                        DownloadService downloadService = Framework.getService(DownloadService.class);
                        String url = downloadService.getDownloadUrl(up, USER_PROFILE_AVATAR_FIELD, avatar.getFilename());
                        jg.writeStringField("avatar", ctx.getBaseUrl() + url);
                    } else {
                        jg.writeNullField("avatar");
                    }
                } finally {
                    jg.writeEndObject();
                }
            }
        }
    }
}
