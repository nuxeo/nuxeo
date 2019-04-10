/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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

    public static final String TENANT_ID_PROPERTY = TENANT_CONFIG_SCHEMA
            + ":tenantId";

    public static final String TENANT_ADMINISTRATORS_PROPERTY = TENANT_CONFIG_SCHEMA
            + ":administrators";

    public static final String TENANTS_DIRECTORY = "tenants";

    // tenant-tenantid_tenantAdministrators
    public static final String TENANT_GROUP_PREFIX = "tenant-";

    public static final String TENANT_ADMINISTRATORS_GROUP_SUFFIX = "_tenantAdministrators";

}
