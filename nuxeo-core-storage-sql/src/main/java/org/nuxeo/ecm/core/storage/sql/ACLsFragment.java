/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.storage.sql.db.Column;

/**
 * A type of fragment corresponding to several rows forming a list of ACLs.
 *
 * @author Florent Guillaume
 */
public class ACLsFragment extends ArrayFragment {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an empty {@link ACLsFragment} of the given table with the
     * given id (which may be a temporary one).
     *
     * @param tableName the table name
     * @param id the id
     * @param state the initial state for the fragment
     * @param context the persistence context to which the row is tied, or
     *            {@code null}
     * @param array the initial acls to use
     */
    public ACLsFragment(Serializable id, State state, Context context,
            Serializable[] acls) {
        super(id, state, context, acls);
    }

    @SuppressWarnings("hiding")
    protected static final CollectionMaker MAKER = new CollectionMaker() {

        public Serializable[] makeArray(Serializable id, ResultSet rs,
                List<Column> columns, Context context, Model model)
                throws SQLException {

            ArrayList<ACLRow> list = new ArrayList<ACLRow>();
            while (rs.next()) {
                int pos = 0;
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
                    if (key.equals(model.ACL_POS_KEY)) {
                        pos = v == null ? 0 : ((Long) v).intValue();
                    } else if (key.equals(model.ACL_NAME_KEY)) {
                        name = (String) v;
                    } else if (key.equals(model.ACL_GRANT_KEY)) {
                        grant = v == null ? false
                                : ((Boolean) v).booleanValue();
                    } else if (key.equals(model.ACL_PERMISSION_KEY)) {
                        permission = (String) v;
                    } else if (key.equals(model.ACL_USER_KEY)) {
                        user = (String) v;
                    } else if (key.equals(model.ACL_GROUP_KEY)) {
                        group = (String) v;
                    } else if (key.equals(model.MAIN_KEY)) {
                        // skip
                    } else {
                        throw new AssertionError(key);
                    }
                }
                ACLRow acl = new ACLRow(pos, name, grant, permission, user,
                        group);
                list.add(acl);
            }
            return list.toArray(new ACLRow[list.size()]);
        }

        public CollectionFragment makeCollection(Serializable id,
                Serializable[] array, Context context) {
            return new ACLsFragment(id, State.PRISTINE, context, array);
        }

        public final ACLRow[] EMPTY_ACL_ROWS = new ACLRow[0];

        public CollectionFragment makeEmpty(Serializable id, Context context,
                Model model) {
            return new ACLsFragment(id, State.CREATED, context, EMPTY_ACL_ROWS);
        }

    };

    @Override
    protected CollectionFragmentIterator getIterator() {
        return new ACLsFragmentIterator();
    }

    protected class ACLsFragmentIterator extends ArrayFragmentIterator {

        @Override
        public void setToPreparedStatement(List<Column> columns,
                PreparedStatement ps, Model model,
                List<Serializable> debugValues) throws SQLException {
            ACLRow acl = (ACLRow) array[i];
            int n = 0;
            for (Column column : columns) {
                n++;
                String key = column.getKey();
                Serializable v;
                if (key.equals(model.MAIN_KEY)) {
                    v = getId();
                } else if (key.equals(model.ACL_POS_KEY)) {
                    v = Long.valueOf(acl.pos);
                } else if (key.equals(model.ACL_NAME_KEY)) {
                    v = acl.name;
                } else if (key.equals(model.ACL_GRANT_KEY)) {
                    v = Boolean.valueOf(acl.grant);
                } else if (key.equals(model.ACL_PERMISSION_KEY)) {
                    v = acl.permission;
                } else if (key.equals(model.ACL_USER_KEY)) {
                    v = acl.user;
                } else if (key.equals(model.ACL_GROUP_KEY)) {
                    v = acl.group;
                } else {
                    throw new AssertionError(key);
                }
                column.setToPreparedStatement(ps, n, v);
                if (debugValues != null) {
                    debugValues.add(v);
                }
            }
        }

    }
}
