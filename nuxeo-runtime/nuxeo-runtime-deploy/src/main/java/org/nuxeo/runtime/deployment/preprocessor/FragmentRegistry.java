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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.deployment.preprocessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.collections.DependencyTree;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FragmentRegistry extends
        DependencyTree<String, FragmentDescriptor> {

    // this is needed to handle requiredBy dependencies
    protected final Map<String, FragmentDescriptor> fragments = new HashMap<String, FragmentDescriptor>();

    public void add(FragmentDescriptor fragment) {
        fragments.put(fragment.name, fragment);
    }

    @Override
    public List<Entry<String, FragmentDescriptor>> getResolvedEntries() {
        if (!fragments.isEmpty()) {
            commitFragments();
        }
        return super.getResolvedEntries();
    }

    @Override
    public List<Entry<String, FragmentDescriptor>> getMissingRequirements() {
        if (!fragments.isEmpty()) {
            commitFragments();
        }
        return super.getMissingRequirements();
    }

    @Override
    public FragmentDescriptor get(String key) {
        if (!fragments.isEmpty()) {
            commitFragments();
        }
        return super.get(key);
    }

    @Override
    public Collection<Entry<String, FragmentDescriptor>> getEntries() {
        if (!fragments.isEmpty()) {
            commitFragments();
        }
        return super.getEntries();
    }

    @Override
    public List<FragmentDescriptor> getResolvedObjects() {
        if (!fragments.isEmpty()) {
            commitFragments();
        }
        return super.getResolvedObjects();
    }

    @Override
    public List<FragmentDescriptor> getPendingObjects() {
        if (!fragments.isEmpty()) {
            commitFragments();
        }
        return super.getPendingObjects();
    }

    @Override
    public Entry<String, FragmentDescriptor> getEntry(String key) {
        if (!fragments.isEmpty()) {
            commitFragments();
        }
        return super.getEntry(key);
    }

    @Override
    public List<Entry<String, FragmentDescriptor>> getPendingEntries() {
        if (!fragments.isEmpty()) {
            commitFragments();
        }
        return super.getPendingEntries();
    }

    protected void commitFragments() {

        // update requires depending on requiredBy
        for (FragmentDescriptor fd : fragments.values()) {
            if (fd.requiredBy != null && fd.requiredBy.length > 0) {
                for (String reqBy : fd.requiredBy) {
                    FragmentDescriptor fdRegBy = fragments.get(reqBy);
                    if (fdRegBy != null) {
                        if (fdRegBy.requires == null) {
                            fdRegBy.requires = new ArrayList<String>();
                        }
                        fdRegBy.requires.add(fd.name);
                    }
                }
            }
        }

        // add fragments to the dependency tree
        for (FragmentDescriptor fd : fragments.values()) {
            add(fd.name, fd, fd.requires);
        }

        // add the "all" marker fragment
        add(FragmentDescriptor.ALL.name, FragmentDescriptor.ALL,
                (Collection<String>) null);

        fragments.clear();
    }

}
