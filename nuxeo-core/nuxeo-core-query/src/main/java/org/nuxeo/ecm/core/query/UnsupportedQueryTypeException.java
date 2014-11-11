/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
