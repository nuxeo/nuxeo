/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benjamin Jalon
 */
package org.nuxeo.ecm.platform.usermanager.local.configuration;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.usermanager.DefaultUserMultiTenantManagement;

public class DefaultUserMultiTenantManagementMock extends DefaultUserMultiTenantManagement {

    @Override
    protected String getDirectorySuffix(DocumentModel context) {
        if (context == null) {
            return null;
        }
        return "-tenanta";
    }
}
