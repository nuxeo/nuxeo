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

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.runtime.model.Adaptable;

/**
 * Base class for rendering contexts.
 * <p>
 * The org.nuxeo.runtime.model.Adaptable interface may be used to add adapting capabilities to context objects. This may
 * be used to retrieve adapters to other type of contexts (like the freemarker one).
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public interface RenderingContext extends Map<String, Object>, Adaptable, Serializable {

    /**
     * Either or not this rendering context accepts the given engine.
     * <p>
     * If the engione is not acepted it will be ignored by the rendering service when processing this context
     *
     * @param engine the engine to test
     * @return true if the engine is eligible to process this context, false otherwise
     */
    boolean accept(RenderingEngine engine);

}
