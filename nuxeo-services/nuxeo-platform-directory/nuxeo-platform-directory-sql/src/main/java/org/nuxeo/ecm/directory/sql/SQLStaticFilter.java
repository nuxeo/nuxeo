/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.directory.sql;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.sql.repository.Column;
import org.nuxeo.ecm.directory.sql.repository.FieldMapper;

@XObject(value = "staticFilter")
public class SQLStaticFilter implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @XNode("column")
    protected String column;

    @XNode("operator")
    protected String operator = "=";

    @XNode("value")
    protected String value;

    @XNode("type")
    protected String type="string";


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

    public Column getDirectoryColumn() throws DirectoryException {
        return new Column(column,FieldMapper.getSqlField(type),column);
    }
}
