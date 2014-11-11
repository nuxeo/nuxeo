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

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.List;

/**
 * A serializable list of document models.
 * <p>
 * It may include information about which part of a bigger list it represents.
 *
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public interface DocumentModelList extends List<DocumentModel>, Serializable {

    /**
     * Returns the total size of the bigger list this is a part of.
     *
     * @return the total size
     */
    long totalSize();

}
