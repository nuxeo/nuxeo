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
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet;

import org.nuxeo.ecm.webengine.jaxrs.servlet.config.ServletDescriptor;

/**
 * Can be implemented by managed servlets that belong to a bundle to have the servlet descriptor
 * injected by the container just before the init() method is called.
 * <p>
 * The descriptor can be used to retrieve the bundle declaring the servlet or other settings.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ManagedServlet {

    public void setDescriptor(ServletDescriptor sd);

}
