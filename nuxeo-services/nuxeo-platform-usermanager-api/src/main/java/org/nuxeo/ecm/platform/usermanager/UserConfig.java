/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;

/**
 * A class containing the configuration of an user principal instance. This class keeps the keys of the basic user
 * fields.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class UserConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final UserConfig DEFAULT = new UserConfig();

    /**
     * The default key name for user id, configurable on the UserManager service
     */
    public static final String USERNAME_COLUMN = "username";

    /**
     * The default key name for user email, configurable on the UserManager service
     */
    public static final String EMAIL_COLUMN = "email";

    public static final String FIRSTNAME_COLUMN = "firstName";

    public static final String LASTNAME_COLUMN = "lastName";

    public static final String COMPANY_COLUMN = "company";

    public static final String PASSWORD_COLUMN = "password";

    public static final String GROUPS_COLUMN = "groups";

    /**
     * @since 8.1
     */
    public static final String TENANT_ID_COLUMN = "tenantId";

    public static final String SCHEMA_NAME = "user";

    public String nameKey = USERNAME_COLUMN;

    public String passwordKey = PASSWORD_COLUMN;

    public String firstNameKey = FIRSTNAME_COLUMN;

    public String lastNameKey = LASTNAME_COLUMN;

    public String companyKey = COMPANY_COLUMN;

    public String emailKey = EMAIL_COLUMN;

    public String groupsKey = GROUPS_COLUMN;

    /**
     * @since 8.1
     */
    public String tenantIdKey = TENANT_ID_COLUMN;

    public String schemaName = SCHEMA_NAME;

    @Override
    public UserConfig clone()
    {
        UserConfig usrCfg = new UserConfig();
        usrCfg.companyKey = companyKey;
        usrCfg.emailKey = emailKey;
        usrCfg.firstNameKey = firstNameKey;
        usrCfg.groupsKey = groupsKey;
        usrCfg.lastNameKey = lastNameKey;
        usrCfg.nameKey = nameKey;
        usrCfg.passwordKey = passwordKey;
        usrCfg.tenantIdKey = tenantIdKey;
        usrCfg.schemaName = schemaName;

        return usrCfg;
    }

}
