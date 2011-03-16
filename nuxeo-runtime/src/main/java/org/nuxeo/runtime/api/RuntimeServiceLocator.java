/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api;

import java.util.Properties;

/**
 * A server implementation that use Nuxeo Runtime to lookup services.
 * <p>
 * This is used as the default server if no other is specified.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RuntimeServiceLocator implements ServiceLocator {

    private static final long serialVersionUID = -3550824536420353831L;

    @Override
    public void initialize(String host, int port, Properties properties)
            throws Exception {
        // do nothing
    }

    @Override
    public void dispose() {
        // do nothing
    }

    @Override
    public Object lookup(ServiceDescriptor svc) throws Exception {
        return Framework.getLocalService(svc.getServiceClass());
    }

    @Override
    public Object lookup(String serviceId) throws Exception { //TODO
        throw new UnsupportedOperationException("not yet implemented");
    }

}
