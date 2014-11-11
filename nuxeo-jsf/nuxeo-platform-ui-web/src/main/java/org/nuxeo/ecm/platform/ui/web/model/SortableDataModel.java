/*
 * (C) Copyright 2006 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse  License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: SortableDataModel.java 11041 2007-01-31 15:43:30Z atchertchian $
 */

package org.nuxeo.ecm.platform.ui.web.model;

import java.io.Serializable;

/**
 * Provides support for sorting table models. Inspired from Tomahawk examples.
 * Abstract method design pattern.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
public interface SortableDataModel extends Serializable {

    /**
     * Sort the list. Should be implemented by the children to customize the
     * sort (what comparators should be used, what other condition must be met).
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
