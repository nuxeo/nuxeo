/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.utils.URIUtils;

/**
 * @since 5.8
 */
public class URLUtilsTest {

    HttpServletRequest req = mock(HttpServletRequest.class);

    HttpServletResponse resp = mock(HttpServletResponse.class);

    @Before
    public void doBefore() {
        when(req.getRequestDispatcher(anyString())).thenReturn(mock(RequestDispatcher.class));
    }

    @Test
    public void testAPIServletForwarding() throws Exception {
        APIServlet servlet = new APIServlet();

        when(req.getContextPath()).thenReturn("/nuxeo");
        when(req.getRequestURI()).thenReturn("/nuxeo/api/path/doc%20with%20space/");
        servlet.service(req, resp);
        verify(req).getRequestDispatcher("/site/api/path/doc%20with%20space/");

        when(req.getRequestURI()).thenReturn("/nuxeo/api/path/default-domain/@children");
        servlet.service(req, resp);
        verify(req).getRequestDispatcher("/site/api/path/default-domain/@children");

        String encodedDocName = URIUtils.quoteURIPathComponent("test ; doc [with] some #, $, :, ; &? and =+", false,
                false);
        when(req.getRequestURI()).thenReturn("/nuxeo/api/path/default-domain/" + encodedDocName);
        servlet.service(req, resp);
        verify(req).getRequestDispatcher("/site/api/path/default-domain/" + encodedDocName);
    }

}
