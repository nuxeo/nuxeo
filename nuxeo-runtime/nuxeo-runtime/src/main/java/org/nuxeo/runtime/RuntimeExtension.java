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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime;

/**
 * A runtime extension is a service that is started by the runtime when it starts using the runtime service context.
 * <p>
 * Runtime Extensions may be used to add new functionality to the runtime service.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface RuntimeExtension {

    /**
     * Starts the runtime extension.
     * <p>
     * The current runtime is available as <code>Runtime.getRuntime()</code>.
     */
    void start();

    /**
     * Stops the runtime extension.
     * <p>
     * The current runtime is available as <code>Runtime.getRuntime()</code>.
     */
    void stop();

}
