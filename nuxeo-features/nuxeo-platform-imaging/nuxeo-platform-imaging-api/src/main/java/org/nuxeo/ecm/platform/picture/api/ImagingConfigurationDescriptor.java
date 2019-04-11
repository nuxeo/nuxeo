/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 *
 */
package org.nuxeo.ecm.platform.picture.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for configuration information contribution that will be used by the ImagingService.
 *
 * @author btatar
 */
@XObject(value = "configuration")
public class ImagingConfigurationDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> parameters = new HashMap<>();

    public Map<String, String> getParameters() {
        return parameters;
    }

}
