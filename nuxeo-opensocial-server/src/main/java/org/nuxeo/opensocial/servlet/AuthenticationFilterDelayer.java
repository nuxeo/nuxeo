/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.opensocial.servlet;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.auth.AuthenticationServletFilter;
import org.osgi.framework.FrameworkEvent;

/**
 * This class is just a wrapper to hold the initialization of the shindig
 * AuthenticationServletFilter because it cannot run without Guice and we have
 * delayed the Guice initialization.
 *
 * @see org.nuxeo.opensocial.servlet.ContextListenerDelayer
 *
 * @author Ian Smith<ismith@nuxeo.com>
 *
 */
public class AuthenticationFilterDelayer implements Filter {

    /*
     * This is the true authentication filter but we have to do some work to
     * delay its Guice-based initialization.
     */
    protected AuthenticationServletFilter delayed = new AuthenticationServletFilter();

    protected boolean ready = false;

    protected FilterConfig delayedConfig;

    private static final Log log = LogFactory.getLog(AuthenticationFilterDelayer.class);

    /*
     * In some packagings (jetty) this object is initialized AFTER the framework
     * is ready, so we need to know if the framework is ready before us.
     */
    protected static boolean hasBeenActivated = false;

    /*
     * We do not create this object, the web container does so we are forced to
     * keep track of all the objects that are so created. This list should be of
     * size 1, but we handle any number the same way.
     */
    private static ArrayList<AuthenticationFilterDelayer> created = new ArrayList<AuthenticationFilterDelayer>();

    public void destroy() {
        delayed.destroy();
    }

    public AuthenticationFilterDelayer() {
        created.add(this);
    }

    /*
     * In normal operation, this is just a pass through to the wrapped and
     * "true" AuthenticationFilter.
     *
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (ready) {
            delayed.doFilter(request, response, chain);
        } else {
            log.warn("received web request prior to framework being "
                    + "fully initialized!");
            chain.doFilter(request, response);
        }
    }

    /*
     * The wrapped object expects to get this message now, but we delay it until
     * we receive the right FrameworkEvent.
     *
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        delayedConfig = filterConfig;
        if (hasBeenActivated) {
            // no point in delaying, everybody is already ready
            try {
                delayed.init(filterConfig);
            } catch (UnavailableException e) {
                log.error(e.getMessage());
                return;
            }
            // we are also now in the ready state because no sense in waiting
            ready = true;
        }
    }

    /*
     * Do the work that should have happened init time. This also "turns on" the
     * filter so future calls pass directly through this class to the wrapped
     * filter.
     */
    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() != FrameworkEvent.STARTED) {
            return;
        }
        try {
            delayed.init(delayedConfig);
            ready = true;
        } catch (ServletException e) {
            log.error(e);
        }
    }

    /*
     * Note: This is static! This echos the framework event to all the instances
     * of this class.
     *
     * @param event framework event (such as "we are started now")
     */
    public static void activate(FrameworkEvent event) {
        // this list will be size 0 when we are activated BEFORE any
        // objects are created
        for (AuthenticationFilterDelayer delayer : created) {
            delayer.frameworkEvent(event);
        }
        hasBeenActivated = true;
    }
}
