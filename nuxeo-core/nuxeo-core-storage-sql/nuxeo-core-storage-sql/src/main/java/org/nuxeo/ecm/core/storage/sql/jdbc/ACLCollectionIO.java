/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.nuxeo.ecm.core.storage.sql.ACLRow;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;

/**
 * Collection IO for arrays of ACLs.
 */
public class ACLCollectionIO implements CollectionIO {

    public static final CollectionIO INSTANCE = new ACLCollectionIO();

    @Override
    public ACLRow getCurrentFromResultSet(ResultSet rs, List<Column> columns,
            Model model, Serializable[] returnId, int[] returnPos)
            throws SQLException {
        Serializable id = null;
        String name = null;
        boolean grant = false;
        String permission = null;
        String user = null;
        String group = null;
        int i = 0;
        for (Column column : columns) {
            i++;
            String key = column.getKey();
            Serializable v = column.getFromResultSet(rs, i);
            if (key.equals(model.MAIN_KEY)) {
                id = v;
            } else if (key.equals(model.ACL_NAME_KEY)) {
                name = (String) v;
            } else if (key.equals(model.ACL_GRANT_KEY)) {
                grant = v == null ? false : (Boolean) v;
            } else if (key.equals(model.ACL_PERMISSION_KEY)) {
                permission = (String) v;
            } else if (key.equals(model.ACL_USER_KEY)) {
                user = (String) v;
            } else if (key.equals(model.ACL_GROUP_KEY)) {
                group = (String) v;
            } else {
                throw new RuntimeException(key);
            }
        }
        Serializable prevId = returnId[0];
        returnId[0] = id;
        int pos = (id != null && !id.equals(prevId)) ? 0 : returnPos[0] + 1;
        returnPos[0] = pos;
        return new ACLRow(pos, name, grant, permission, user, group);
    }

    @Override
    public void setToPreparedStatement(Serializable id, Serializable[] array,
            List<Column> columns, PreparedStatement ps, Model model,
            List<Serializable> debugValues, String sql, JDBCMapperLogger logger)
            throws SQLException {
        for (int i = 0; i < array.length; i++) {
            ACLRow acl = (ACLRow) array[i];
            int n = 0;
            for (Column column : columns) {
                n++;
                String key = column.getKey();
                Serializable v;
                if (key.equals(model.MAIN_KEY)) {
                    v = id;
                } else if (key.equals(model.ACL_POS_KEY)) {
                    v = (long) acl.pos;
                } else if (key.equals(model.ACL_NAME_KEY)) {
                    v = acl.name;
                } else if (key.equals(model.ACL_GRANT_KEY)) {
                    v = acl.grant;
                } else if (key.equals(model.ACL_PERMISSION_KEY)) {
                    v = acl.permission;
                } else if (key.equals(model.ACL_USER_KEY)) {
                    v = acl.user;
                } else if (key.equals(model.ACL_GROUP_KEY)) {
                    v = acl.group;
                } else {
                    throw new RuntimeException(key);
                }
                column.setToPreparedStatement(ps, n, v);
                if (debugValues != null) {
                    debugValues.add(v);
                }
            }
            if (debugValues != null) {
                logger.logSQL(sql, debugValues);
                debugValues.clear();
            }
            ps.execute();
        }
    }

}
