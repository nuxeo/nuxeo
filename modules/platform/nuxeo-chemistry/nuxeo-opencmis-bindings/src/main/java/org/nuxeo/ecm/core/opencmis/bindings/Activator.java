/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.core.opencmis.bindings;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

/**
 * This bundle activator ensures that the init sequence happens in the right order.
 */
public class Activator implements BundleActivator, FrameworkListener {

    /*
     * Called when our bundle is started. All we do is ask for an event when the entire Framework is ready.
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext )
     */
    @Override
    public void start(BundleContext context) {
        context.addFrameworkListener(this);
    }

    @Override
    public void stop(BundleContext context) {

    }

    /*
     * This is the point where the initialization actually occurs. This is called by the framework when it's finished
     * initializing and we echo that message to the object that we have delayed.
     */
    @Override
    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STARTED) {
            ContextListenerDelayer.activate(event);
        }
    }

}
