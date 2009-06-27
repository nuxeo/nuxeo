/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.query;

import java.util.Iterator;

import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public interface QueryResult {

    long count();

    /**
     * Returns the total size the query results would have if no limit and
     * offset was passed.
     *
     * @return the total size
     */
    long getTotalSize();

    boolean isEmpty();

    boolean next();

    /**
     * Retrieves the current row number. (1 based index)
     * <p>
     * If there is no current row (no next() was called) returns 0
     *
     * @return the current row number
     */
    long row();

    String getString(int i) throws QueryException;

    String getString(String column) throws QueryException;

    boolean getBoolean(int i) throws QueryException;

    boolean getBoolean(String column) throws QueryException;

    long getLong(int i, long defaultValue) throws QueryException;

    long getLong(String column, long defaultValue) throws QueryException;

    double getDouble(int i, double defaultValue) throws QueryException;

    double getDouble(String column, double defaultValue) throws QueryException;

    /**
     * Currently not implemented.
     */
    Object getObject(String column) throws QueryException;

    Object getObject() throws QueryException;

    DocumentModelList getDocumentModels() throws QueryException;

    Iterator getDocuments(int start);

}
