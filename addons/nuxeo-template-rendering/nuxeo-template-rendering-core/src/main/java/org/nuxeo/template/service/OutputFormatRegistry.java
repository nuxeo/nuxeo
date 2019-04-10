/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     ldoguin
 *
 */
package org.nuxeo.template.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;
import org.nuxeo.template.api.descriptor.OutputFormatDescriptor;

public class OutputFormatRegistry extends
        ContributionFragmentRegistry<OutputFormatDescriptor> {

    protected Map<String, OutputFormatDescriptor> outputFormats = new HashMap<String, OutputFormatDescriptor>();

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
    public void contributionUpdated(String id,
            OutputFormatDescriptor outFormat,
            OutputFormatDescriptor newoutFormat) {
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
    public void merge(OutputFormatDescriptor srcOutFormat,
            OutputFormatDescriptor descOutFormat) {
        descOutFormat.merge(srcOutFormat);
    }

}
