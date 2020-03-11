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
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.slf4j.LoggerFactory;

/**
 * @author dmetzler
 * @since 5.7
 */
public class QuotaSizeServiceImpl extends DefaultComponent implements QuotaSizeService {

    private Set<String> excludedPathList = new HashSet<>();

    private static Logger LOG = LoggerFactory.getLogger(QuotaSizeServiceImpl.class);

    @Override
    public Collection<String> getExcludedPathList() {
        return excludedPathList;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if ("exclusions".equals(extensionPoint)) {
            BlobExcludeDescriptor descriptor = (BlobExcludeDescriptor) contribution;
            LOG.info(String.format("Adding %s to size quota computation's blacklist", descriptor.getPathRegexp()));
            excludedPathList.add(descriptor.getPathRegexp());
        }

    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if ("exclusions".equals(extensionPoint)) {
            BlobExcludeDescriptor descriptor = (BlobExcludeDescriptor) contribution;
            String pathRegexp = descriptor.getPathRegexp();
            if (excludedPathList.contains(pathRegexp)) {
                LOG.info(String.format("Removing %s from size quota computation's blacklist", pathRegexp));
                excludedPathList.remove(pathRegexp);

            }
        }
    }

}
