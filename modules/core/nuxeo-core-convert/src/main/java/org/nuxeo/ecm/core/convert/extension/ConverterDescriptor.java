/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.convert.extension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * XMap descriptor for the contribution of a new {@link Converter}.
 *
 * @author tiry
 */
@XObject("converter")
@XRegistry
public class ConverterDescriptor {

    protected final Log log = LogFactory.getLog(ConverterDescriptor.class);

    public static final String CUSTOM_CONVERTER_TYPE = "Custom";

    public static final String CHAINED_CONVERTER_TYPE = "Chain";

    @XNode("@name")
    @XRegistryId
    protected String converterName;

    @XNodeList(value = "sourceMimeType", type = ArrayList.class, componentType = String.class)
    protected List<String> sourceMimeTypes = new ArrayList<>();

    @XNode("destinationMimeType")
    protected String destinationMimeType;

    @XNode("@class")
    protected Class<?> className;

    @XNode("@type")
    protected String converterType = CUSTOM_CONVERTER_TYPE;

    @XNode("@bypassIfSameMimeType")
    protected boolean bypassIfSameMimeType;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters = new HashMap<>();

    @XNodeList(value = "conversionSteps/step", type = ArrayList.class, componentType = String.class)
    protected List<String> steps = new ArrayList<>();

    @XNodeList(value = "conversionSteps/subconverter", type = ArrayList.class, componentType = String.class)
    protected List<String> subConverters = new ArrayList<>();

    /**
     * Returns whether the conversion should be bypassed if the input blob mime type equals the converter destination
     * mime type.
     *
     * @since 11.1
     */
    public boolean isBypassIfSameMimeType() {
        return bypassIfSameMimeType;
    }

    public String getConverterName() {
        return converterName;
    }

    public List<String> getSourceMimeTypes() {
        return sourceMimeTypes;
    }

    public List<String> getSteps() {
        return steps;
    }

    public String getDestinationMimeType() {
        return destinationMimeType;
    }

    public Converter getConverterInstance() throws ReflectiveOperationException {
        Converter instance;
        if (className == null || converterType.equals(CHAINED_CONVERTER_TYPE)) {
            if (subConverters == null || subConverters.isEmpty()) {
                // create a Chained converter based on mimetypes
                instance = new ChainedConverter();
            } else {
                // create a Chained converter based on converter chain
                instance = new ChainedConverter(subConverters);
            }
            converterType = CHAINED_CONVERTER_TYPE;
        } else {
            instance = (Converter) className.getDeclaredConstructor().newInstance();
        }
        instance.init(this);
        return instance;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getConverterType() {
        return converterType;
    }

}
