/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.fm;

import static java.util.stream.Collectors.toMap;

import java.util.Map;

import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FreemarkerComponent extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(FreemarkerComponent.class.getName());

    /** @since 2025.14 */
    protected static final String SETTINGS_EXTENSION_POINT = "settings";

    /** @since 2025.14 */
    public Map<String, String> getFreemarkerSettings() {
        return this.<FreemarkerSettingDescriptor> getDescriptors(SETTINGS_EXTENSION_POINT)
                   .stream()
                   .collect(toMap(FreemarkerSettingDescriptor::getName, FreemarkerSettingDescriptor::getValue));
    }

    public FreemarkerEngine newEngine() {
        return new FreemarkerEngine();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == FreemarkerComponent.class) {
            return adapter.cast(this);
        }
        return null;
    }

}
