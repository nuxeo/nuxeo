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

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getInstallationMessage() {
        return installationMessage;
    }

    public void setInstallationMessage(String installationMessage) {
        this.installationMessage = installationMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<String> getSupportedInputMimeTypes() {
        return supportedInputMimeTypes;
    }

    public void setSupportedInputMimeTypes(List<String> supportedInputMimeTypes) {
        this.supportedInputMimeTypes = supportedInputMimeTypes;
    }

}
