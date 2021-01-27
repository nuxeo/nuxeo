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
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XMerge;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.template.api.TemplateProcessor;

@XObject("templateProcessor")
@XRegistry(enable = false)
public class TemplateProcessorDescriptor {

    protected static final Log log = LogFactory.getLog(TemplateProcessorDescriptor.class);

    protected TemplateProcessor processor;

    @XNode("@name")
    @XRegistryId
    protected String name;

    @XNode("@label")
    protected String label;

    @XNode("@class")
    protected Class<?> className;

    @XNode("@default")
    protected boolean defaultProcessor = true;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled")
    @XEnable
    protected boolean enabled;

    @XNodeList(value = "supportedMimeType", type = ArrayList.class, componentType = String.class)
    @XMerge(value = "@mergeSupportedMimeTypes")
    protected List<String> supportedMimeTypes = new ArrayList<>();

    @XNodeList(value = "supportedExtension", type = ArrayList.class, componentType = String.class)
    @XMerge(value = "@mergeSupportedExtensions")
    protected List<String> supportedExtensions = new ArrayList<>();

    public boolean init() {
        return getProcessor() == null;
    }

    public TemplateProcessor getProcessor() {
        if (processor == null && className != null) {
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

}
