/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Ronan DANIELLOU
 */
package org.nuxeo.targetplatforms.api.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.targetplatforms.api.TargetInfo;
import org.nuxeo.targetplatforms.api.TargetPlatformFilter;

/**
 * This filter accepts input data that is accepted by at least one of its filters.
 *
 * @since 1.0.74, 1.11.6033
 */
public class TargetPlatformOrFilterImpl implements TargetPlatformFilter {

    private static final long serialVersionUID = 1L;

    Set<TargetPlatformFilter> filters;

    public TargetPlatformOrFilterImpl() {
        this(null);
    }

    public TargetPlatformOrFilterImpl(Collection<TargetPlatformFilter> filters) {
        this.filters = new HashSet<TargetPlatformFilter>();
        if (filters != null) {
            this.filters.addAll(filters);
        }
    }

    public void addFilter(TargetPlatformFilter filter) {
        filters.add(filter);
    }

    @Override
    public boolean accepts(TargetInfo t) {
        for (TargetPlatformFilter filter : filters) {
            if (filter.accepts(t)) {
                return true;
            }
        }
        return false;
    }

}
