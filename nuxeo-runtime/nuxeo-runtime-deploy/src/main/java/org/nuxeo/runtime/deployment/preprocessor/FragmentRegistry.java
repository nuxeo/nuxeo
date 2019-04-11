/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.DependencyTree;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FragmentRegistry extends DependencyTree<String, FragmentDescriptor> {

    private static final Log log = LogFactory.getLog(FragmentRegistry.class);

    // this is needed to handle requiredBy dependencies
    protected final Map<String, FragmentDescriptor> fragments = new HashMap<>();

    public void add(FragmentDescriptor fragment) {
        if (fragments.containsKey(fragment.name)) {
            FragmentDescriptor existing = fragments.get(fragment.name);
            log.error(String.format("Overriding fragment with name '%s' and path '%s' "
                    + "that is already present with path '%s'", fragment.name, fragment.filePath, existing.filePath));
        }
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
                            fdRegBy.requires = new ArrayList<>();
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
        add(FragmentDescriptor.ALL.name, FragmentDescriptor.ALL, (Collection<String>) null);

        fragments.clear();
    }

}
