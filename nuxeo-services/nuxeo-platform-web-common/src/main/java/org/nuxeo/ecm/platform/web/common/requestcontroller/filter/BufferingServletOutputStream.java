/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.web.common.requestcontroller.filter;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @deprecated since 7.3, use {@link org.nuxeo.ecm.core.io.download.BufferingServletOutputStream} instead.
 */
@Deprecated
public class BufferingServletOutputStream {

    /**
     * @deprecated since 7.3, use
     *             {@link org.nuxeo.ecm.core.io.download.BufferingServletOutputStream#stopBuffering(OutputStream)}
     *             instead
     */
    @Deprecated
    public static void stopBuffering(OutputStream out) throws IOException {
        org.nuxeo.ecm.core.io.download.BufferingServletOutputStream.stopBuffering(out);
    }


}
