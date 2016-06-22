/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.platform.web.common;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.Test;

/**
 * @since 5.7.2
 */
public class TestContentDisposition {

    private static final String MSIE_7 = "Mozilla/4.0 (compatible; MSIE 7.0)";

    @Test
    public void inlineContentDisposition() throws Exception {
        for (boolean useAttribute : new Boolean[] { true, false }) {

            HttpServletRequest req = getRequest(useAttribute, "true", MSIE_7);
            assertEquals("inline; filename=myfile.txt", ServletHelper.getRFC2231ContentDisposition(req, "myfile.txt"));

            req = getRequest(useAttribute, "false", MSIE_7);
            assertEquals("attachment; filename=myfile.txt",
                    ServletHelper.getRFC2231ContentDisposition(req, "myfile.txt"));

            req = getRequest(useAttribute, null, MSIE_7);
            assertEquals("attachment; filename=myfile.txt",
                    ServletHelper.getRFC2231ContentDisposition(req, "myfile.txt"));
        }
    }

    private HttpServletRequest getRequest(boolean useAttribute, String inline, String userAgent) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        if (useAttribute) {
            when(req.getAttribute("inline")).thenReturn(inline);
        } else {
            when(req.getParameter("inline")).thenReturn(inline);
        }
        when(req.getHeader("User-Agent")).thenReturn(userAgent);
        return req;
    }
}
