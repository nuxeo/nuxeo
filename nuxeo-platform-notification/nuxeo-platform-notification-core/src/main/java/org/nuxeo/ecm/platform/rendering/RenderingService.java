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

import java.util.Collection;

/**
 * RenderingService core infrastructure.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public interface RenderingService {

    /**
     * Processes the given context and returns a collection of results.
     *
     * @param ctx the context
     * @return the result
     * @throws RenderingException
     */
    Collection<RenderingResult> process(RenderingContext ctx)
            throws RenderingException;

    RenderingEngine getEngine(String format);

    void registerEngine(RenderingEngine engine);

    void unregisterEngine(String format);

}
