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
 *     Miguel Nixo
 */
package org.nuxeo.ecm.platform.ec.notification.io;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.ec.notification.SubscriptionAdapter;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.List;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

/**
 * Enricher that lists the current user's subscribed notifications for a particular document.
 *
 * @since 8.10
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class NotificationsJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "subscribedNotifications";

    public NotificationsJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        jg.writeFieldName(NAME);
        jg.writeStartArray();
        try (RenderingContext.SessionWrapper wrapper = ctx.getSession(document)) {
            String username = NotificationConstants.USER_PREFIX + wrapper.getSession().getPrincipal().getName();
            List<String> notifications = document.getAdapter(SubscriptionAdapter.class).getUserSubscriptions(username);
            for (String notification : notifications) {
                jg.writeString(notification);
            }
        }
        jg.writeEndArray();
    }

}
