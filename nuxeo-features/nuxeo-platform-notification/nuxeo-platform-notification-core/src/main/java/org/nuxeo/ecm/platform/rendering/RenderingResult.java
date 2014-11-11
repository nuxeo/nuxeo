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

import java.io.InputStream;
import java.io.Serializable;

import org.nuxeo.runtime.model.Adaptable;


/**
 * A rendering result is an object that wraps a rendering result and give several methods
 * to retrieve the rendering outcome.
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
     * Gets the format name of the result. This can be use to identify the
     * type of the result. The format name can be a mime type or any
     * application-defined format.
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
