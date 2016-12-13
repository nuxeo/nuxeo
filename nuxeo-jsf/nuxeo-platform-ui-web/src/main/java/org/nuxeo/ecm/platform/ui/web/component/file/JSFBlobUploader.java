/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ui.web.component.file;

import java.io.IOException;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

/**
 * Interface for a Blob Uploader, a JSF helper that knows how to encode and validate a component to provide blob input
 * in the JSF UI.
 *
 * @since 7.2
 */
public interface JSFBlobUploader {

    /**
     * Gets the choice key associated to this uploader. This MUST start with "upload" for the display logic to work
     * correctly.
     */
    String getChoice();

    /**
     * Constructs the needed subcomponent.
     */
    void hookSubComponent(UIInput parent);

    /**
     * Generates the HTML for an upload choice.
     */
    void encodeBeginUpload(UIInput parent, FacesContext context, String onChange) throws IOException;

    /**
     * Transforms input into a blob.
     *
     * @param submitted the value into which the input is stored
     */
    void validateUpload(UIInput parent, FacesContext context, InputFileInfo submitted);

    /**
     * Checks if the uploader is enabled. Only enabled uploaders are added to the UI.
     *
     * @since 7.4
     */
    boolean isEnabled();

}
