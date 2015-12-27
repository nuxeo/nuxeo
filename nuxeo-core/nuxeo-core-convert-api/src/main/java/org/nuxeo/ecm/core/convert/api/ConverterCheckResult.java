/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.convert.api;

import java.io.Serializable;
import java.util.List;

/**
 * Result object for an availability check on a Converter.
 * <p>
 * Contains an availability flag + error and installation message is needed.
 *
 * @author tiry
 */
public class ConverterCheckResult implements Serializable {

    private static final long serialVersionUID = 1L;

    protected boolean available;

    protected String installationMessage;

    protected String errorMessage;

    protected List<String> supportedInputMimeTypes;

    public ConverterCheckResult() {
        available = true;
    }

    public ConverterCheckResult(String installationMessage, String errorMessage) {
        available = false;
        this.installationMessage = installationMessage;
        this.errorMessage = errorMessage;
    }

    public boolean isAvailable() {
        return available;
    }

    // Never used. Remove?
    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getInstallationMessage() {
        return installationMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<String> getSupportedInputMimeTypes() {
        return supportedInputMimeTypes;
    }

    public void setSupportedInputMimeTypes(List<String> supportedInputMimeTypes) {
        this.supportedInputMimeTypes = supportedInputMimeTypes;
    }

}
