/*
 * (C) Copyright 2006 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: SortableDataModel.java 11041 2007-01-31 15:43:30Z atchertchian $
 */

package org.nuxeo.ecm.platform.ui.web.model;

import java.io.Serializable;

/**
 * Provides support for sorting table models. Inspired from Tomahawk examples. Abstract method design pattern.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
public interface SortableDataModel extends Serializable {

    /**
     * Sort the list. Should be implemented by the children to customize the sort (what comparators should be used, what
     * other condition must be met).
     */
    void sort(String column, boolean ascending);

    /**
     * Is the default sort direction for the given column "ascending" ?
     */
    boolean isDefaultAscending(String sortColumn);

    void sort(String sortColumn);

    String getSort();

    void setSort(String sort);

    boolean isAscending();

    void setAscending(boolean ascending);

}
