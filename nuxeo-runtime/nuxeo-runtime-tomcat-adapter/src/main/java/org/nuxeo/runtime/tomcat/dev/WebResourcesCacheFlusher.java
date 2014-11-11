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
 *     slacoin
 */
package org.nuxeo.runtime.tomcat.dev;

import javax.management.MXBean;

/**
 * Flush resources in web-app class loader, needed for resetting the i18n.
 * <p>
 * A runtime event listener propagates the same flush to the tomcat context,
 * see {@link WebResourcesReloadHandler}.
 *
 * @see SeamHotReloadHelper#flush
 * @since 5.5
 */
@MXBean
public interface WebResourcesCacheFlusher {

    void flushWebResources();

}
