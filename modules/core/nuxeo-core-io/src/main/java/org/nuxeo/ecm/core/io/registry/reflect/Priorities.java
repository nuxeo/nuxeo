/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.registry.reflect;

/**
 * Some priorities constants used for annotation's property {@link Setup#priority()}.
 * <p>
 * {@link #REFERENCE} is the default priority for Nuxeo marshallers. If you want to override a Nuxeo marshaller, be very
 * carreful about consequences and use an higher priority like {@link #OVERRIDE_REFERENCE}.
 * </p>
 *
 * @since 7.2
 */
public interface Priorities {

    int DEFAULT = 0;

    int DERIVATIVE = 1000;

    /**
     * Most of Nuxeo builtin marshallers.
     */
    int REFERENCE = 2000;

    int OVERRIDE_REFERENCE = 3000;

}
