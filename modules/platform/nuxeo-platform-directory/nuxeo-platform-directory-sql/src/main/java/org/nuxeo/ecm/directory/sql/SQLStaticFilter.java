/*
 * (C) Copyright 2010-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.sql;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;

@XObject(value = "staticFilter")
public class SQLStaticFilter implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("column")
    protected String column;

    @XNode("operator")
    protected String operator = "=";

    @XNode("value")
    protected String value;

    @XNode("type")
    protected String type = "string";

    public String getType() {
        return type;
    }

    public String getColumn() {
        return column;
    }

    public String getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    public Column getDirectoryColumn(Table table, boolean nativeCase) {
        return table.getColumn(column);
    }

    @Override
    public SQLStaticFilter clone() {
        SQLStaticFilter clone = new SQLStaticFilter();
        clone.column = column;
        clone.operator = operator;
        clone.value = value;
        clone.type = type;
        return clone;
    }

}
