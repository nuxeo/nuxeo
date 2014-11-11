/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.tx;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.webengine.jaxrs.HttpFilter;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Filter using the {@link SimpleLoginModule} to authenticate a request.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TransactionFilter extends HttpFilter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void run(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        boolean txStarted = false;
        if (!TransactionHelper.isTransactionActive())  {
            if (TransactionHelper.startTransaction()) {
                txStarted = true;
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            if (txStarted) {
                TransactionHelper.commitOrRollbackTransaction();
            }
        }
    }

    @Override
    public void destroy() {
    }

}
