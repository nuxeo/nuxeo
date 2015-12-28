/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthPreFilter;

public class NuxeoAuthFilterChain implements FilterChain {

    protected List<NuxeoAuthPreFilter> preFilters = new ArrayList<NuxeoAuthPreFilter>();

    protected NuxeoAuthenticationFilter mainFilter;

    protected FilterChain standardFilterChain;

    public NuxeoAuthFilterChain(List<NuxeoAuthPreFilter> preFilters, FilterChain standardFilterChain,
            NuxeoAuthenticationFilter mainFilter) {
        this.preFilters.addAll(preFilters);
        this.mainFilter = mainFilter;
        this.standardFilterChain = standardFilterChain;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (preFilters != null && !preFilters.isEmpty()) {
            NuxeoAuthPreFilter preFilter = preFilters.remove(0);
            preFilter.doFilter(request, response, this);
        } else {
            mainFilter.doFilterInternal(request, response, standardFilterChain);
        }
    }

}
