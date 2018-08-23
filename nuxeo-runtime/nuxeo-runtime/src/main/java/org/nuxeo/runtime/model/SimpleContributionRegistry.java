/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple contribution registry, keeping up to date contributions in a map, and not handling merge.
 *
 * @since 5.6
 * @deprecated since 10.3 use DefaultComponent descriptors management methods instead
 */
@Deprecated
public abstract class SimpleContributionRegistry<T> extends ContributionFragmentRegistry<T> {

    protected Map<String, T> currentContribs = new HashMap<>();

    @Override
    public void contributionUpdated(String id, T contrib, T newOrigContrib) {
        currentContribs.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id, T origContrib) {
        currentContribs.remove(id);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public T clone(T orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(T src, T dst) {
        throw new UnsupportedOperationException();
    }

    protected T getCurrentContribution(String id) {
        return currentContribs.get(id);
    }

}
