/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.platform.pictures.tiles.tilers;

import org.apache.commons.lang3.SystemUtils;

/**
 * Dummy base class for PictureTilers
 *
 * @author tiry
 */
public abstract class BasePictureTiler implements PictureTiler {

    /**
     * @deprecated Since 7.4. Use {@link SystemUtils#IS_OS_WINDOWS}
     */
    @Deprecated
    protected static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

}
