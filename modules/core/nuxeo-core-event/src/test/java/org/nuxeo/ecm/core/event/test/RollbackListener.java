/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.core.event.test;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;

import java.io.Serializable;
import java.util.Map;

/**
 * @since 11.5
 */
public class RollbackListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        Map<String, Serializable> properties = event.getContext().getProperties();

        event.markRollBack();
        if (properties.get("RuntimeException") != null) {
            throw new RuntimeException("RuntimeException");
        }
        throw new NuxeoException("NuxeoException", 400);
    }

}
