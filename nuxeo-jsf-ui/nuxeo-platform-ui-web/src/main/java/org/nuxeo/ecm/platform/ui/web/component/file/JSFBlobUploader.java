/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
