/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.api.impl;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.targetplatforms.api.TargetInfo;
import org.nuxeo.targetplatforms.api.TargetPlatformFilter;

/**
 * Filter of target platforms handling enabled, deprecated, restricted and type criteria.
 *
 * @since 5.7.1
 */
public class TargetPlatformFilterImpl implements TargetPlatformFilter {

    private static final long serialVersionUID = 1L;

    protected boolean filterDisabled = false;

    protected boolean filterRestricted = false;

    protected boolean filterDeprecated = false;

    protected boolean filterDefault = false;

    protected boolean filterNotTrial = false;

    protected String filterType;

    public TargetPlatformFilterImpl() {
        super();
    }

    public TargetPlatformFilterImpl(boolean filterDisabled, boolean filterRestricted, boolean filterDeprecated,
            boolean filterDefault, String filterType) {
        super();
        this.filterDisabled = filterDisabled;
        this.filterRestricted = filterRestricted;
        this.filterDeprecated = filterDeprecated;
        this.filterDefault = filterDefault;
        this.filterType = filterType;
    }

    public TargetPlatformFilterImpl(boolean filterNotTrial) {
        super();
        this.filterNotTrial = filterNotTrial;
    }

    public void setFilterDisabled(boolean filterDisabled) {
        this.filterDisabled = filterDisabled;
    }

    public void setFilterRestricted(boolean filterRestricted) {
        this.filterRestricted = filterRestricted;
    }

    public void setFilterDeprecated(boolean filterDeprecated) {
        this.filterDeprecated = filterDeprecated;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    public void setFilterDefault(boolean filterDefault) {
        this.filterDefault = filterDefault;
    }

    public void setFilterNotTrial(boolean filterNotTrial) {
        this.filterNotTrial = filterNotTrial;
    }

    @Override
    public boolean accepts(TargetInfo t) {
        if (t == null) {
            return false;
        }
        if ((filterDisabled && !t.isEnabled()) || (filterDeprecated && t.isDeprecated())
                || (filterRestricted && t.isRestricted()) || (filterNotTrial && !t.isTrial())
                || (filterDefault && !t.isDefault())
                || (!StringUtils.isBlank(filterType) && !t.matchesType(filterType))) {
            return false;
        }
        return true;
    }

}
