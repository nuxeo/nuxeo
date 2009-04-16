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
 * $Id: ResultItem.java 28480 2008-01-04 14:04:49Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.search.results;

import java.io.Serializable;
import java.util.Map;

/**
 * Result item.
 * <p>
 * Result items are stored on ResultSet instances.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface ResultItem extends Map<String, Serializable> {

    /**
     * Return the name of the result item.
     *
     * @return the name of the result item.
     */
    String getName();

}
