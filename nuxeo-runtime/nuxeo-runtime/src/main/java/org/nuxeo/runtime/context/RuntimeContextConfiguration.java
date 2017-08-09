/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 *
 */
package org.nuxeo.runtime.context;

import java.util.Collections;
import java.util.List;

import org.nuxeo.runtime.services.config.ConfigurationPropertyDescriptor;

/**
 * TODO remove this class, it is used currently to explore the way to declare component pojo.
 *
 * @since 9.3
 */
@Component
public class RuntimeContextConfiguration {

    @Descriptor(target = "org.nuxeo.runtime.ConfigurationService", point = "configuration")
    public ConfigurationPropertyDescriptor myConfiguration() {
        return new ConfigurationPropertyDescriptor("nuxeo.runtime.context.descriptor", Boolean.TRUE.toString());
    }

    @Descriptor(target = "org.nuxeo.runtime.ConfigurationService", point = "configuration")
    public List<ConfigurationPropertyDescriptor> myConfigurations() {
        return Collections.singletonList(
                new ConfigurationPropertyDescriptor("nuxeo.runtime.context.descriptor.list", Boolean.TRUE.toString()));
    }

}
