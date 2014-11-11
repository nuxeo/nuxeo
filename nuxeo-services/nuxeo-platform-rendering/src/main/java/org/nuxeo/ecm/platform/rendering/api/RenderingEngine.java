/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.api;

import java.io.Writer;
import java.util.ResourceBundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface RenderingEngine {

    void setMessageBundle(ResourceBundle messages);

    ResourceBundle getMessageBundle();

    void setResourceLocator(ResourceLocator locator);

    ResourceLocator getResourceLocator();

    void setSharedVariable(String key, Object value);

    /**
     * Starts the rendering for the given document context.
     *
     * @throws RenderingException
     */
    void render(String template, Object input, Writer writer)
            throws RenderingException;

}
