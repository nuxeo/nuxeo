/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple contribution registry, keeping up to date contributions in a map, and
 * not handling merge.
 *
 * @since 5.6
 */
public abstract class SimpleContributionRegistry<T> extends
        ContributionFragmentRegistry<T> {

    protected Map<String, T> currentContribs = new HashMap<String, T>();

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
