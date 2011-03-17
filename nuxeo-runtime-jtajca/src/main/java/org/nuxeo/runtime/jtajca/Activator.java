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
 *     bstefanescu
 */
package org.nuxeo.runtime.jtajca;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.nuxeo.common.jndi.NamingContextFactory;
import org.nuxeo.runtime.api.Framework;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * If this bundle is present in the running platform it should automatically install
 * the NuxeoContainer.
 *
 * TODO: enable this activator when other distributions are tested and works correctly or use a framework property to activate it.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Activator implements BundleActivator {

    public static final String AUTO_ACTIVATION = "NuxeoContainer.autoactivation";

    @Override
    public void start(BundleContext context) throws Exception {
        // instantiate it only on demand through the AUTO_ACTIVATION runtime property since
        // it may break the application in test mode or in other non osgi distributions
        // where the container is explicitly activated
        // TODO: use this activation method in all distributions too.
        if ("true".equalsIgnoreCase(Framework.getProperty(AUTO_ACTIVATION))) {
            // if no InitialContext exists install the dummy one.
            try {
                new InitialContext();
            } catch (NamingException e) {
                NamingContextFactory.install();
            }
            NuxeoContainer.install();
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        NuxeoContainer.uninstall();
    }

}
