/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.UserVisiblePermission;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.runtime.api.Framework;

/**
 * Enrich {@link DocumentModel} Json.
 * <p>
 * Add document user visible permissions as json attachment.
 * </p>
 * <p>
 * Enable if parameter enrichers.document=userVisiblePermissions is present.
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
        List<UserVisiblePermission> userVisiblePermissions = permissionProvider.getUserVisiblePermissionDescriptors(
                document.getType());

        jg.writeArrayFieldStart(NAME);
        for (UserVisiblePermission permission : userVisiblePermissions) {
            jg.writeString(permission.getId());
        }
        jg.writeEndArray();
    }

}
