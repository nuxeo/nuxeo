/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     ldoguin
 *
 */
package org.nuxeo.template.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;
import org.nuxeo.template.api.descriptor.OutputFormatDescriptor;

public class OutputFormatRegistry extends ContributionFragmentRegistry<OutputFormatDescriptor> {

    protected Map<String, OutputFormatDescriptor> outputFormats = new HashMap<>();

    @Override
    public OutputFormatDescriptor clone(OutputFormatDescriptor outFormat) {
        return outFormat.clone();
    }

    public OutputFormatDescriptor getOutputFormatById(String id) {
        return outputFormats.get(id);
    }

    public Collection<OutputFormatDescriptor> getRegistredOutputFormat() {
        return outputFormats.values();
    }

    @Override
    public void contributionRemoved(String id, OutputFormatDescriptor outFormat) {
        outputFormats.remove(id);
    }

    @Override
    public void contributionUpdated(String id, OutputFormatDescriptor outFormat, OutputFormatDescriptor newoutFormat) {
        if (outFormat == null || !outFormat.isEnabled()) {
            outputFormats.remove(id);
        } else {
            outputFormats.put(id, outFormat);
        }
    }

    @Override
    public String getContributionId(OutputFormatDescriptor outFormat) {
        return outFormat.getId();
    }

    @Override
    public void merge(OutputFormatDescriptor srcOutFormat, OutputFormatDescriptor descOutFormat) {
        descOutFormat.merge(srcOutFormat);
    }

}
