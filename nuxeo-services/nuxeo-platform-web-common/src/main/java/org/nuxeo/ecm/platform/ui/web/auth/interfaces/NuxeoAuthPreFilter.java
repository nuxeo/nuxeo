/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.ui.web.auth.interfaces;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * The Authentication filter already provides a complex plugin model. Unfortunately in some cases, it's easier to have a
 * dedicated filter to implement the custom auth logic. But in this case, you have to configure the new filter for each
 * url pattern that is already protected by NuxeoAuthenticationFilter.
 * <p>
 * In order to avoid that you can run your Filter as a pre-Filter for the NuxeoAuthenticationFilter. For that you need
 * to implement this interface and register your implementation via the preFilter extension point.
 *
 * @author tiry
 */
public interface NuxeoAuthPreFilter {

    /**
     * Main Filter method {@see Filter}. The FilterChain is only composed of the preFilters and the
     * NuxeoAuthenticationFilter
     *
     * @see FilterChain
     */
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException;

}
