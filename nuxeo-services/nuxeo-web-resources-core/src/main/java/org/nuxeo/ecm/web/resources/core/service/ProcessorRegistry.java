/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.web.resources.core.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.web.resources.api.Processor;
import org.nuxeo.ecm.web.resources.core.ProcessorDescriptor;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for processors registry.
 *
 * @since 7.3
 */
public class ProcessorRegistry extends ContributionFragmentRegistry<ProcessorDescriptor> {

    private static final Log log = LogFactory.getLog(ProcessorRegistry.class);

    protected final Map<String, ProcessorDescriptor> processors = new HashMap<String, ProcessorDescriptor>();

    @Override
    public String getContributionId(ProcessorDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, ProcessorDescriptor contrib, ProcessorDescriptor newOrigContrib) {
        if (processors.containsKey(id)) {
            processors.remove(id);
        }
        if (contrib.isEnabled()) {
            processors.put(id, contrib);
            log.info("Registering processor with name " + id);
        }
    }

    @Override
    public void contributionRemoved(String id, ProcessorDescriptor origContrib) {
        processors.remove(id);
        log.info("Unregistering processor with name " + id);
    }

    @Override
    public ProcessorDescriptor clone(ProcessorDescriptor orig) {
        return orig.clone();
    }

    @Override
    public void merge(ProcessorDescriptor src, ProcessorDescriptor dst) {
        dst.merge(src);
    }

    // custom API

    public Processor getProcessor(String id) {
        return processors.get(id);
    }

    public List<Processor> getProcessors() {
        return getProcessors(null);
    }

    public List<Processor> getProcessors(String type) {
        List<Processor> res = new ArrayList<>();
        Collection<ProcessorDescriptor> all = processors.values();
        for (Processor proc : all) {
            if (type == null || proc.getTypes().contains(type)) {
                res.add(proc);
            }
        }
        Collections.sort(res);
        return res;
    }

}
