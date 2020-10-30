/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;

/**
 * @since 9.3
 */
public class GroupConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final GroupConfig DEFAULT = new GroupConfig();

    public static final String DEFAULT_SCHEMA_NAME = "group";

    public static final String DEFAULT_ID_FIELD = "groupname";

    public static final String DEFAULT_LABEL_FIELD = "grouplabel";

    public static final String DEFAULT_MEMBERS_FIELD = "members";

    public static final String DEFAULT_SUB_GROUPS_FIELD = "subGroups";

    public static final String DEFAULT_PARENT_GROUPS_FIELD = "parentGroups";

    public String idField = DEFAULT_ID_FIELD;

    public String labelField = DEFAULT_LABEL_FIELD;

    public String membersField = DEFAULT_MEMBERS_FIELD;

    public String subGroupsField = DEFAULT_SUB_GROUPS_FIELD;

    public String parentGroupsField = DEFAULT_PARENT_GROUPS_FIELD;

    public String schemaName = DEFAULT_SCHEMA_NAME;
}
