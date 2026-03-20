/*
 * (C) Copyright 2012-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.api.descriptor;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;
import org.nuxeo.template.api.TemplateProcessor;

@XObject("templateProcessor")
public class TemplateProcessorDescriptor implements Descriptor {

    private static final Logger log = LogManager.getLogger(TemplateProcessorDescriptor.class);

    @XNode("@name")
    protected String name;

    @XNode("@label")
    protected String label;

    @SuppressWarnings("rawtypes")
    @XNode("@class")
    protected Class<?> className;

    @XNode("@default")
    protected boolean defaultProcessor = true;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNodeList(value = "supportedMimeType", type = ArrayList.class, componentType = String.class)
    protected List<String> supportedMimeTypes = new ArrayList<>();

    @XNodeList(value = "supportedExtension", type = ArrayList.class, componentType = String.class)
    protected List<String> supportedExtensions = new ArrayList<>();

    protected TemplateProcessor processor;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    @SuppressWarnings("rawtypes")
    public Class getClassName() {
        return className;
    }

    public boolean isDefaultProcessor() {
        return defaultProcessor;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getSupportedMimeTypes() {
        return supportedMimeTypes;
    }

    public List<String> getSupportedExtensions() {
        return supportedExtensions;
    }

    public TemplateProcessor getProcessor() {
        if (processor == null) {
            try {
                processor = (TemplateProcessor) className.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                log.error("Unable to instantiate Processor", e);
            }
        }
        return processor;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (TemplateProcessorDescriptor) o;
        var merged = new TemplateProcessorDescriptor();
        merged.name = name; // we merge based on name, so no name merging needed
        merged.label = defaultIfBlank(other.label, label);
        merged.className = getIfNull(other.className, className);
        merged.defaultProcessor = other.defaultProcessor;
        merged.enabled = other.enabled;
        merged.supportedMimeTypes = !other.supportedMimeTypes.isEmpty() ? other.supportedMimeTypes : supportedMimeTypes;
        merged.supportedExtensions = !other.supportedExtensions.isEmpty() ? other.supportedExtensions
                : supportedExtensions;
        return merged;
    }
}
