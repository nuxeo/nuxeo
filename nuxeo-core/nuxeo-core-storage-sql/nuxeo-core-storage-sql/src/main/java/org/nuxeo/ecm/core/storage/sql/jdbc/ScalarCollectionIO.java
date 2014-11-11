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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.storage.sql.ACLRow;
import org.nuxeo.ecm.core.storage.sql.CollectionFragment;
import org.nuxeo.ecm.core.storage.sql.Context;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Fragment.State;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;

/**
 * Collection IO for arrays of scalar values.
 */
public class ScalarCollectionIO implements CollectionIO {

    public static final CollectionIO INSTANCE = new ScalarCollectionIO();

    public Serializable getCurrentFromResultSet(ResultSet rs,
            List<Column> columns, Model model, Serializable[] returnId)
            throws SQLException {
        Serializable value = null;
        int i = 0;
        for (Column column : columns) {
            i++;
            String key = column.getKey();
            Serializable v = column.getFromResultSet(rs, i);
            if (key.equals(model.MAIN_KEY)) {
                if (returnId != null) {
                    returnId[0] = v;
                }
            } else if (key.equals(model.COLL_TABLE_POS_KEY)) {
                // (the pos column is ignored, results are already ordered by id
                // then pos)
            } else if (key.equals(model.COLL_TABLE_VALUE_KEY)) {
                value = v;
            } else {
                throw new RuntimeException(key);
            }
        }
        return value;
    }

    public void setToPreparedStatement(CollectionFragment fragment,
            List<Column> columns, PreparedStatement ps, Model model,
            List<Serializable> debugValues, String sql, JDBCMapperLogger logger)
            throws SQLException {
        for (int i = 0; i < fragment.array.length; i++) {
            int n = 0;
            for (Column column : columns) {
                n++;
                String key = column.getKey();
                Serializable v;
                if (key.equals(model.MAIN_KEY)) {
                    v = fragment.getId();
                } else if (key.equals(model.COLL_TABLE_POS_KEY)) {
                    v = (long) i;
                } else if (key.equals(model.COLL_TABLE_VALUE_KEY)) {
                    v = fragment.array[i];
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
