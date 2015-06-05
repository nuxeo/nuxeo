/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

    /**
     * @deprecated since 7.3, use
     *             {@link org.nuxeo.ecm.core.io.download.BufferingServletOutputStream#stopBufferingThread} instead
     */
    @Deprecated
    public static void stopBufferingThread() throws IOException {
        org.nuxeo.ecm.core.io.download.BufferingServletOutputStream.stopBufferingThread();
    }

}
