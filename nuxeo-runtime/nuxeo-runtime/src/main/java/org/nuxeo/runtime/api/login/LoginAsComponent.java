/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.runtime.api.login;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;

/**
 * LoginAs component.
 * <p>
 * It allows to contribute implementation to which login is delegated.
 *
 * @since 10.3
 */
public class LoginAsComponent extends DefaultComponent {

    private static final Logger log = LogManager.getLogger(LoginAsComponent.class);

    private static final String XP_IMPL = "implementation";

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == LoginAs.class) {
            return (T) getLoginAs();
        }
        return null;
    }

    protected LoginAs getLoginAs() {
        LoginAsDescriptor descriptor = getDescriptor(XP_IMPL, Descriptor.UNIQUE_DESCRIPTOR_ID);
        if (descriptor == null) {
            return null;
        }
        try {
            return descriptor.klass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            log.error("Unable to instantiate LoginAs implementation", e);
            return null;
        }
    }

}
