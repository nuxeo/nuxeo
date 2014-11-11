/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime.reload;

/**
 * Copy of event names as triggered by the ReloadService, to make them
 * available on the web layer.
 *
 * @since 5.6
 */
public class ReloadEventNames {

    public static final String FLUSH_EVENT_ID = "flush";

    public static final String FLUSH_SEAM_EVENT_ID = "flushSeamComponents";

    public static final String RELOAD_EVENT_ID = "reload";

    public static final String RELOAD_SEAM_EVENT_ID = "reloadSeamComponents";

}
