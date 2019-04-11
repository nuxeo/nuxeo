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

import java.util.ArrayList;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.osgi.framework.FrameworkEvent;

/**
 * This class is just a wrapper to hold the initialization of Nuxeo CMIS until we have received the "go ahead" from the
 * Runtime and that everything is fully initialized.
 */
public class ContextListenerDelayer implements ServletContextListener {

    /*
     * this is the true object but because of differences in initialization sequence we have to delay this until nuxeo
     * is fully up and all the user's configuration files have been loaded into extension points
     */

    protected NuxeoCmisContextListener delayed = new NuxeoCmisContextListener();

    /*
     * Saved from the original call so we can feed it to the true object later
     */
    protected ServletContextEvent delayedEvent;

    /*
     * We don't call the constructor directly -- the servlet container does so we have to hold a list of all the objects
     * of this type created. this number should be 1, be we track "all of them" just in case.
     */
    private static ArrayList<ContextListenerDelayer> created = new ArrayList<>();

    /*
     * We have to keep track of whether or not activate has been called already because in some packagings (jetty) the
     * framework ready method is called BEFORE the war container instantiates the objects. Sigh.
     */
    protected static boolean hasBeenActivated = false;

    /*
     * No need to delay this method.
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        delayed.contextDestroyed(sce);
    }

    /*
     * Make the list of all objects created of this type.
     */
    public ContextListenerDelayer() {
        created.add(this);
    }

    /*
     * This is where the true object expects to be initialzed but we prevent it.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        delayedEvent = sce;
        if (hasBeenActivated) {
            // we are running AFTER the framework is created so no
            // sense in delaying further
            delayed.contextInitialized(sce);
        }
    }

    /*
     * Do the work that should have happened at contextInitialized() now if the Framework is fully up.
     */
    public void frameworkEvent(FrameworkEvent event) {
        if (delayedEvent == null) {
            // OSGi activation done before ServletContextListener init
            // will initialize later as a standard ServletContextListener
            return;
        }
        if (event.getType() == FrameworkEvent.STARTED) {
            delayed.contextInitialized(delayedEvent);
        }
    }

    /*
     * Walk the list of objects of this type we have created echoing the framework event. Note: This method is static!
     */

    public static void activate(FrameworkEvent event) {
        // this list will be size 0 if no objects were created
        // before the framework is ready, thus we use the
        // has been activated flag
        for (ContextListenerDelayer delayer : created) {
            delayer.frameworkEvent(event);
        }
        hasBeenActivated = true;
    }

}
