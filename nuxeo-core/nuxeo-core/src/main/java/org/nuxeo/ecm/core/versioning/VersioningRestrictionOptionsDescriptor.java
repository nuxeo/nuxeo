/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.versioning;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.VersioningOption;

/**
 * Descriptor used to restrict versioning options for a given lifeCycleState
 *
 * @since 9.1
 */
@XObject("options")
public class VersioningRestrictionOptionsDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@lifeCycleState")
    protected String lifeCycleState;

    @XNodeList(value = "option", componentType = OptionDescriptor.class, type = ArrayList.class)
    protected List<OptionDescriptor> optionDescriptors;

    public String getLifeCycleState() {
        return lifeCycleState;
    }

    public List<VersioningOption> getOptions() {
        List<VersioningOption> options = new LinkedList<>();
        for (OptionDescriptor optionDescriptor : optionDescriptors) {
            if (optionDescriptor.defaultOpt) {
                options.add(0, optionDescriptor.option);
            } else {
                options.add(optionDescriptor.option);
            }
        }
        return options;
    }

    @XObject("option")
    protected static class OptionDescriptor implements Serializable {

        private static final long serialVersionUID = 1L;

        @XNode("@default")
        protected boolean defaultOpt;

        @XNode
        protected VersioningOption option;

    }

}
