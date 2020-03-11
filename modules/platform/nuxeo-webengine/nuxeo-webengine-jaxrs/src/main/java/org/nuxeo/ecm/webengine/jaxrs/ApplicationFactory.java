/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.core.Application;

import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ApplicationFactory {

    /**
     * Create a new application instance given the bundle declaring the application and the attributes specified in the
     * manifest.
     *
     * @param bundle the bundle defining the application
     * @param args the arguments parsed from manifest WebModule entry.
     */
    Application getApplication(Bundle bundle, Map<String, String> args) throws ReflectiveOperationException,
            IOException;

}
