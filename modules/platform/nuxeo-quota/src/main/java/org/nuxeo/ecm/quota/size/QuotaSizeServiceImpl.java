/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.quota.size;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author dmetzler
 * @since 5.7
 */
public class QuotaSizeServiceImpl extends DefaultComponent implements QuotaSizeService {

    protected static final String XP = "exclusions";

    private Set<String> exclusions;

    @Override
    public void start(ComponentContext context) {
        exclusions = this.<BlobExcludeDescriptor> getRegistryContributions(XP)
                         .stream()
                         .map(BlobExcludeDescriptor::getPathRegexp)
                         .collect(Collectors.toSet());
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        exclusions = null;
    }

    @Override
    public Collection<String> getExcludedPathList() {
        return Collections.unmodifiableCollection(exclusions);
    }

}
