/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.schema;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

/**
 * @since 5.7
 */
public class FlushPendingsRegistrationOnReloadListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        if (!"reload".equals(event.getId())) {
            return;
        }
        SchemaManager mgr = Framework.getLocalService(SchemaManager.class);
        ((SchemaManagerImpl) mgr).flushPendingsRegistration();
    }

}
