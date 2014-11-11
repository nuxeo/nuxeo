/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * The org.nuxeo.runtime.model.Adaptable interface may be used to add adapting
 * capabilities to context objects. This may be used to retrieve adapters to
 * other type of contexts (like the freemarker one).
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public interface RenderingContext extends Map<String, Object>, Adaptable, Serializable {

    /**
     * Either or not this rendering context accepts the given engine.
     * <p>
     * If the engione is not acepted it will be ignored by the rendering service
     * when processing this context
     *
     * @param engine the engine to test
     * @return true if the engine is eligible to process this context, false otherwise
     */
    boolean accept(RenderingEngine engine);

}
