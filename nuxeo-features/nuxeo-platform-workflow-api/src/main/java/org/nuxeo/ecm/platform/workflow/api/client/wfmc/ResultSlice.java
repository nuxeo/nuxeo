/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id:$
 */

package org.nuxeo.ecm.platform.workflow.api.client.wfmc;

import java.util.List;

/**
 * Data transfer class to hold a slice of a result set of items of type E along
 * with the total number of results that could have been fetched. This is
 * especially useful for paginated results of queries for workitem instances.
 *
 * @author ogrisel
 *
 * @param <E> Type of the elements of the embedded collection of result
 */
public class ResultSlice<E> {

    public final List<E> slice;

    public final int firstResult;

    public final int maxResult;

    // FIXME: totalResult or totalResults ?
    public final int totalResult;

    public ResultSlice(List<E> slice, int firstResult, int maxResult,
            int totalResults) {
        this.slice = slice;
        this.firstResult = firstResult;
        this.maxResult = maxResult;
        this.totalResult = totalResults;
    }

}
