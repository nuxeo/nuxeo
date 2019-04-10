/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.quota.count;

/**
 * Constants for documents count statistics.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class Constants {

    private Constants() {
        // Constants class
    }

    public static final String DOCUMENTS_COUNT_STATISTICS_FACET = "DocumentsCountStatistics";

    public static final String DOCUMENTS_COUNT_STATISTICS_CHILDREN_COUNT_PROPERTY = "dcs:childrenCount";

    public static final String DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY = "dcs:descendantsCount";
}
