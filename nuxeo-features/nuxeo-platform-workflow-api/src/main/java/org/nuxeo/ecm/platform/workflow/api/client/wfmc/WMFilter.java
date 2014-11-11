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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: WMFilter.java 28498 2008-01-05 11:46:25Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.api.client.wfmc;

import java.io.Serializable;

/**
 * Filter interface.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface WMFilter extends Serializable {

    // Ops constants.

    int EQ = 0;

    int GE = 1;

    int GT = 2;

    int LE = 3;

    int LT = 4;

    int NE = 5;

    /**
     * Returns the attribute against to filter.
     *
     * @return the attribute name as a string
     */
    String getAttributeName();

    /**
     * Returns the comparison operator.
     *
     * @return a constant operator.
     */
    int getComparison();

    /**
     * Return the filter value
     *
     * <p>
     *  Backend will handle instance type.
     * </p>
     *
     * @return the filter value
     */
    Serializable getFilterValue();

}
