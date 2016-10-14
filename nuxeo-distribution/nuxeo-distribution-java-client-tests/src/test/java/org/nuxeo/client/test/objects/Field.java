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
 *         Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.client.test.objects;

import java.util.List;
import java.util.Map;

/**
 * @since 1.0
 */
public class Field {

    String fieldType;

    String description;

    List<String> roles;

    String columnName;

    String sqlTypeHint;

    String name;

    public Field(String fieldType, String description, List<String> roles, String columnName, String sqlTypeHint,
            String name) {
        this.fieldType = fieldType;
        this.description = description;
        this.roles = roles;
        this.columnName = columnName;
        this.sqlTypeHint = sqlTypeHint;
        this.name = name;
    }

    public Field(Object field) {
        this.fieldType = (String) ((Map)field).get("fieldType");
        this.description = (String) ((Map)field).get("description");
        this.columnName = (String) ((Map)field).get("columnName");
        this.sqlTypeHint = (String) ((Map)field).get("sqlTypeHint");
        this.name = (String) ((Map)field).get("name");
        this.roles = (List<String>) ((Map)field).get("roles");
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getSqlTypeHint() {
        return sqlTypeHint;
    }

    public void setSqlTypeHint(String sqlTypeHint) {
        this.sqlTypeHint = sqlTypeHint;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
