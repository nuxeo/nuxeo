/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.opensocial.gadgets.service.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This is how code outside of the gadget implementation sees the gadget. The
 * implementation can be an internal or external gadget. Callers should not
 * depend on particular implementation strategies since they may vary quite
 * widely.
 *
 * @author Ian Smith<iansmith@nuxecloud.com>
 *
 */
public interface GadgetDeclaration {

    String getName();

    boolean getDisabled();

    String getCategory();

    String getIconUrl();

    InputStream getResourceAsStream(String resourcePath) throws IOException;

    URL getGadgetDefinition() throws MalformedURLException;
}
