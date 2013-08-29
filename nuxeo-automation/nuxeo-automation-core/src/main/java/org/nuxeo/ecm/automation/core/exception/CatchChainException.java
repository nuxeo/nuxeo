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
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.exception;

/**
 * @since 5.7.3
 */
public class CatchChainException {

    protected final String chainId;

    protected final Integer priority;

    protected final String filterId;

    protected final Boolean rollBack;

    public CatchChainException() {
        this.chainId = "";
        this.priority = 0;
        this.rollBack = true;
        this.filterId = "";
    }

    public CatchChainException(String chainId, Integer priority, Boolean rollBack,
            String filterId) {
        this.chainId = chainId;
        this.priority = priority;
        this.rollBack = rollBack;
        this.filterId = filterId;
    }

    public String getChainId() {
        return chainId;
    }

    public Integer getPriority() {
        return priority;
    }

    public Boolean getRollBack() {
        return rollBack;
    }

    public String getFilterId() {
        return filterId;
    }

    public Boolean hasFilter() {
        return !filterId.isEmpty();
    }

}
