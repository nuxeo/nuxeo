/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
