/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.multi.tenant;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class Constants {

    private Constants() {
        // Constants class
    }

    public static final String TENANT_CONFIG_FACET = "TenantConfig";

    public static final String TENANT_CONFIG_SCHEMA = "tenantconfig";

    public static final String TENANT_ID_PROPERTY = TENANT_CONFIG_SCHEMA + ":tenantId";

    public static final String TENANT_ADMINISTRATORS_PROPERTY = TENANT_CONFIG_SCHEMA + ":administrators";

    public static final String TENANTS_DIRECTORY = "tenants";

    // tenant-tenantid_tenantAdministrators
    public static final String TENANT_GROUP_PREFIX = "tenant-";

    public static final String TENANT_ADMINISTRATORS_GROUP_SUFFIX = "_tenantAdministrators";

    public static final String TENANT_MEMBERS_GROUP_SUFFIX = "_tenantMembers";

    public static final String POWER_USERS_GROUP = "powerusers";

}
