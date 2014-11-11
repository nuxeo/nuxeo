/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.query;

/**
 * If the query factory instantiating a specific implementation of
 * the Query interface does not support a given Query Type than a
 * <code>UnsupportedQueryTypeException</code> should be thrown.
 *
 * @author DM
 */
public class UnsupportedQueryTypeException extends QueryException {

    private static final long serialVersionUID = -8560755618283893246L;

    public UnsupportedQueryTypeException(Query.Type qtype) {
        super(" for query type " + qtype);
    }

}
