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
 *     Ian Smith
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.opensocial.gadgets.service.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import org.apache.shindig.gadgets.spec.GadgetSpec;

/**
 * This is how code outside of the gadget implementation sees the gadget. The
 * implementation can be an internal or external gadget. Callers should not
 * depend on particular implementation strategies since they may vary quite
 * widely.
 *
 * @author Ian Smith<iansmith@nuxecloud.com>
 * @author Thomas Roger<troger@nuxeo.com>
 *
 */
public interface GadgetDeclaration {

    String getName();

    boolean getDisabled();

    String getCategory();

    String getIconUrl();

    InputStream getResourceAsStream(String resourcePath) throws IOException;

    URL getGadgetDefinition() throws MalformedURLException;

    /**
     * Returns the public URL of the gadget spec. That URL can be used to add
     * the gadget in an other container.
     *
     * @since 5.4.2
     */
    String getPublicGadgetDefinition() throws MalformedURLException;

    boolean isExternal();

    String getDescription();

    GadgetSpec getGadgetSpec();

    String getTitle();

    /**
     * Try to find an internationalized title for this gadget.
     * <p>
     * The label key is "label.gadget." + gadgetName.
     *
     * @since 5.5
     */
    String getTitle(Locale locale);

    /**
     *
     * Try to find an localized description for this gadet
     * <p>
     * The label key is "label.gadget." + gadgetName + ".description"
     *
     * @since 5.8
     */
    String getDescription(Locale locale);

    String getAuthor();

    String getScreenshot();

    String getThumbnail();

    URL getResource(String resourcePath) throws IOException;

}
