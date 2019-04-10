/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 *
 */
package org.nuxeo.ecm.quota.size;

import java.util.Collection;

/**
 * @author dmetzler
 * @since 5.7
 *
 */
public interface QuotaSizeService {

    /**
     * Exposes the list of blob paths that are excluded for size quota computation.
     *
     * @return the list of paths
     * @since 5.7
     */
    Collection<String> getExcludedPathList();

}
