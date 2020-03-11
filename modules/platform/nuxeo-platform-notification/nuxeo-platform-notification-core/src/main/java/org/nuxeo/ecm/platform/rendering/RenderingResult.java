/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dragos
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering;

import java.io.InputStream;
import java.io.Serializable;

import org.nuxeo.runtime.model.Adaptable;

/**
 * A rendering result is an object that wraps a rendering result and give several methods to retrieve the rendering
 * outcome.
 * <p>
 * The default one is to expose the rendering outcome as a stream.
 * <p>
 * Specialized results may be retrieved using {@link Adaptable#getAdapter(Class)} method
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public interface RenderingResult extends Adaptable, Serializable {

    /**
     * Gets the format name of the result. This can be use to identify the type of the result. The format name can be a
     * mime type or any application-defined format.
     *
     * @return the format name
     */
    String getFormatName();

    /**
     * Gets the rendering result as a stream.
     *
     * @return the stream or null if the outcome cannot be expressed as a stream
     */
    InputStream getStream();

    /**
     * Gets the rendering result object.
     *
     * @return the rendering result. must never be null
     */
    Object getOutcome();

}
