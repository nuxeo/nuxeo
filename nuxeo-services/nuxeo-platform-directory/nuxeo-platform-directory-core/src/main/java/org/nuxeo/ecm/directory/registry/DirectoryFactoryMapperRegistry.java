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
package org.nuxeo.ecm.directory.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Tracks what factory should be used depending on the directory registration.
 *
 * @since 5.6
 */
public class DirectoryFactoryMapperRegistry extends ContributionFragmentRegistry<DirectoryFactoryMapper> {

    protected Map<String, List<String>> factoriesByDir = new HashMap<String, List<String>>();

    @Override
    public String getContributionId(DirectoryFactoryMapper contrib) {
        return contrib.getDirectoryName();
    }

    @Override
    public void contributionUpdated(String id, DirectoryFactoryMapper contrib, DirectoryFactoryMapper newOrigContrib) {
        factoriesByDir.put(id, contrib.getFactories());
    }

    @Override
    public void contributionRemoved(String id, DirectoryFactoryMapper origContrib) {
        List<String> factories = factoriesByDir.get(id);
        if (factories != null) {
            List<String> toRemove = origContrib.getFactories();
            if (toRemove != null) {
                factories.removeAll(toRemove);
            }
        }
    }

    @Override
    public DirectoryFactoryMapper clone(DirectoryFactoryMapper orig) {
        return orig.clone();
    }

    @Override
    public void merge(DirectoryFactoryMapper src, DirectoryFactoryMapper dst) {
        // new factories given by src should be added on dst, taking precedence
        // over previous available factories
        List<String> oldFacts = dst.getFactories();
        List<String> updatedFacts = new ArrayList<String>();
        if (oldFacts != null) {
            updatedFacts.addAll(oldFacts);
        }
        List<String> newFacts = src.getFactories();
        if (newFacts != null) {
            for (String newFact : newFacts) {
                // remove if already there
                updatedFacts.remove(newFact);
                // add at the beginning
                updatedFacts.add(0, newFact);
            }
        }
        dst.setFactories(updatedFacts);
    }

    // API

    public List<String> getFactoriesForDirectory(String name) {
        return factoriesByDir.get(name);
    }

}
