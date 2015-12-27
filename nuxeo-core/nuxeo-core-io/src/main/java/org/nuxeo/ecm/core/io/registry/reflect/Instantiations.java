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

import org.nuxeo.ecm.core.io.registry.context.RenderingContext;

/**
 * Define the instantiation mode for this a marshaller.
 * <p>
 * The use of {@link #SINGLETON} is highly recommended.
 * </p>
 * <p>
 * {@link #SINGLETON} marshallers are more priority than others.
 * </p>
 *
 * @since 7.2
 */
public enum Instantiations {

    /**
     * The marshaller is instantiated once.
     * <p>
     * Each class with this instantiation mode should just have thread safe properties, or injected properties.
     * </p>
     * <p>
     * Please note that injected {@link RenderingContext} is thread safe.
     * </p>
     */
    SINGLETON,

    /**
     * One instance of the marshaller is created per thread.
     */
    PER_THREAD,

    /**
     * One instance of marshaller is created for each marshalling request.
     */
    EACH_TIME;

}
