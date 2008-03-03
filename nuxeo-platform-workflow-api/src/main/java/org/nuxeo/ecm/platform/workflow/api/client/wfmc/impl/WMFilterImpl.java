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
 * $Id: WMFilterImpl.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl;

import java.io.Serializable;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMFilter;

/**
 * WMFilter implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class WMFilterImpl implements WMFilter {

    private static final long serialVersionUID = 1L;

    protected String attributeName;

    protected int comparison;

    protected Serializable filterValue;

    public WMFilterImpl() {

    }

    public WMFilterImpl(String attributeName, int comparison,
            Serializable filterValue) {
        this.attributeName = attributeName;
        this.comparison = comparison;
        this.filterValue = filterValue;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public int getComparison() {
        return comparison;
    }

    public Serializable getFilterValue() {
        return filterValue;
    }

}
