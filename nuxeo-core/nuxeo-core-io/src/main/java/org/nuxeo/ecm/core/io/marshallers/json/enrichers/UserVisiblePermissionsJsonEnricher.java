/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.UserVisiblePermission;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Enrich {@link DocumentModel} Json.
 * <p>
 * Add document user visible permissions as json attachment.
 * </p>
 * <p>
 * Enable if parameter enrichers-document=userVisiblePermissions is present.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {@code
 * {
 *   "entity-type":"document",
 *   ...
 *   "contextParameters": {
 *     "userVisiblePermissions": ["Read", "ReadWrite", "Everything"]
 *   }
 * }
 * </pre>
 *
 * </p>
 *
 * @since 7.4
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class UserVisiblePermissionsJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "userVisiblePermissions";

    public UserVisiblePermissionsJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        PermissionProvider permissionProvider = Framework.getService(PermissionProvider.class);
        List<UserVisiblePermission> userVisiblePermissions = permissionProvider.getUserVisiblePermissionDescriptors(document.getType());

        jg.writeArrayFieldStart(NAME);
        for (UserVisiblePermission permission : userVisiblePermissions) {
            jg.writeString(permission.getId());
        }
        jg.writeEndArray();
    }

}
