/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.picture.core;

import java.io.InputStream;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;

/**
 * @author Max Stepanov
 */
public interface ImageUtils {

    @Deprecated
    InputStream crop(InputStream in, int x, int y, int width, int height);

    @Deprecated
    InputStream resize(InputStream in, int width, int height);

    @Deprecated
    InputStream rotate(InputStream in, int angle);

    Blob crop(Blob blob, int x, int y, int width, int height);

    Blob resize(Blob blob, String finalFormat, int width, int height, int depth);

    Blob rotate(Blob blob, int angle);

}
