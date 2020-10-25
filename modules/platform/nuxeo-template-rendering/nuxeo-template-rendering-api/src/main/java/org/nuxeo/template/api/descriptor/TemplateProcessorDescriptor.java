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
 *     Thierry Delprat
 */
package org.nuxeo.template.api.descriptor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.template.api.TemplateProcessor;

@XObject("templateProcessor")
public class TemplateProcessorDescriptor {

    protected static final Log log = LogFactory.getLog(TemplateProcessorDescriptor.class);

    protected TemplateProcessor processor;

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

    public boolean init() {
        if (getProcessor() == null) {
            return false;
        }
        return true;
    }

    public TemplateProcessor getProcessor() {
        if (processor == null) {
            try {
                processor = (TemplateProcessor) className.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                log.error("Unable to instanciate Processor", e);
            }
        }
        return processor;
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

    public List<String> getSupportedMimeTypes() {
        return supportedMimeTypes;
    }

    public List<String> getSupportedExtensions() {
        return supportedExtensions;
    }

    public boolean isDefaultProcessor() {
        return defaultProcessor;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public TemplateProcessorDescriptor clone() {

        TemplateProcessorDescriptor clone = new TemplateProcessorDescriptor();
        clone.enabled = enabled;
        clone.supportedExtensions = supportedExtensions;
        clone.supportedMimeTypes = supportedMimeTypes;
        clone.className = className;
        clone.processor = processor;
        clone.defaultProcessor = defaultProcessor;
        clone.label = label;
        clone.name = name;

        return clone;
    }

    public void merge(TemplateProcessorDescriptor srcTpd) {
        defaultProcessor = srcTpd.defaultProcessor;
        if (srcTpd.className != null) {
            className = srcTpd.className;
        }
        if (srcTpd.label != null) {
            label = srcTpd.label;
        }
        if (srcTpd.supportedExtensions != null && srcTpd.supportedExtensions.size() > 0) {
            supportedExtensions = srcTpd.supportedExtensions;
        }
        if (srcTpd.supportedMimeTypes != null && srcTpd.supportedMimeTypes.size() > 0) {
            supportedMimeTypes = srcTpd.supportedMimeTypes;
        }
    }
}
