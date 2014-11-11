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
package org.nuxeo.ecm.webengine.jaxrs;

import java.util.Map;

import javax.ws.rs.core.Application;

import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ApplicationFactory {

    /**
     * Create a new application instance given the bundle declaring the application
     * and the attributes specified in the manifest.
     *
     * @param bundle the bundle defining the application
     * @param args the arguments parsed from manifest WebModule entry.
     */
    Application getApplication(Bundle bundle, Map<String, String> args) throws Exception;


}
