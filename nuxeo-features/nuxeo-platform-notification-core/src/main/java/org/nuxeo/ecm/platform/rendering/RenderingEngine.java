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

/**
 * A RenderingEngine will be instantiated by the RenderingService according with
 * the descriptor specified for it. The specific implementation of a RenderingEngine
 * must be in classpath for it to be instantiated and used.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public interface RenderingEngine {

    String getFormatName();

    /**
     * Processes the given context and return a rendering result.
     * <p>
     * The processing must never return null. If some error occurs it must
     * throw an exception.
     *
     * @param ctx the context to process
     */
    RenderingResult process(RenderingContext ctx) throws RenderingException;
}
