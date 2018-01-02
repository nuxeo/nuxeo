/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
