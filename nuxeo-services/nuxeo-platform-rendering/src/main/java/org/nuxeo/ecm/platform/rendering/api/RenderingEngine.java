/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
