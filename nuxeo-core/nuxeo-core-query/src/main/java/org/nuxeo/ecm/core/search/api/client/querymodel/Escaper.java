/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.search.api.client.querymodel;

/**
 * An interface for special character escaping in queries for easy configuration of this matter.
 * <p>
 * This is meant for the contents of string literals in where clauses, once they have been extracted. It's therefore not
 * necessary to escape single quotes, unless of course they have some meaning to the search backend.
 * </p>
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 */
public interface Escaper {

    /**
     * Escapes the provided string
     *
     * @param s
     * @return the escaped string
     */
    String escape(String s);
}
