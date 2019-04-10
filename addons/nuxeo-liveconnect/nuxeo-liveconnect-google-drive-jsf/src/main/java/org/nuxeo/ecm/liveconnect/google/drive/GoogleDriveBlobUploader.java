/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nelson Silva
 */
package org.nuxeo.ecm.liveconnect.google.drive;

import java.io.IOException;
import java.util.Locale;

import javax.faces.application.Application;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;
import org.nuxeo.ecm.platform.ui.web.component.file.InputFileChoice;
import org.nuxeo.ecm.platform.ui.web.component.file.InputFileInfo;
import org.nuxeo.ecm.platform.ui.web.component.file.JSFBlobUploader;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.runtime.api.Framework;

import com.google.api.client.auth.oauth2.Credential;

/**
 * JSF Blob Upload based on Google Drive blobs.
 *
 * @since 7.3
 */
public class GoogleDriveBlobUploader implements JSFBlobUploader {

    private static final Log log = LogFactory.getLog(GoogleDriveBlobUploader.class);

    public static final String UPLOAD_GOOGLE_DRIVE_FACET_NAME = "uploadGoogleDrive";

    // restrict sign-in to accounts at this domain
    public static final String GOOGLE_DOMAIN_PROP = "nuxeo.google.domain";

    protected final String id;

    public GoogleDriveBlobUploader(String id) {
        this.id = id;
        try {
            getGoogleDriveBlobProvider();
        } catch (NuxeoException e) {
            // this exception is caught by JSFBlobUploaderDescriptor.getJSFBlobUploader
            // to mean that the uploader is not available because badly configured
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getChoice() {
        return InputFileChoice.UPLOAD + "GoogleDrive";
    }

    @Override
    public void hookSubComponent(UIInput parent) {
        Application app = FacesContext.getCurrentInstance().getApplication();
        ComponentUtils.initiateSubComponent(parent, UPLOAD_GOOGLE_DRIVE_FACET_NAME,
                app.createComponent(HtmlInputText.COMPONENT_TYPE));

    }

    // Needs supporting JavaScript code for nuxeo.utils.pickFromGoogleDrive defined in googleclient.js.
    @Override
    public void encodeBeginUpload(UIInput parent, FacesContext context, String onClick) throws IOException {
        UIComponent facet = parent.getFacet(UPLOAD_GOOGLE_DRIVE_FACET_NAME);
        if (!(facet instanceof HtmlInputText)) {
            return;
        }
        HtmlInputText inputText = (HtmlInputText) facet;

        // not ours to close
        @SuppressWarnings("resource")
        ResponseWriter writer = context.getResponseWriter();

        String inputId = facet.getClientId(context);
        String prefix = parent.getClientId(context) + NamingContainer.SEPARATOR_CHAR;
        String pickId = prefix + "GoogleDrivePickMsg";
        String authId = prefix + "GoogleDriveAuthMsg";
        String infoId = prefix + "GoogleDriveInfo";
        String authorizationUrl = hasServiceAccount() ? "" : getOAuthAuthorizationUrl();
        Locale locale = context.getViewRoot().getLocale();
        String message;
        boolean isProviderAvailable = getGoogleDriveBlobProvider().getOAuth2Provider().isProviderAvailable();

        writer.startElement("button", parent);
        writer.writeAttribute("type", "button", null);
        writer.writeAttribute("class", "button GoogleDrivePickerButton", null);

        // only add onclick event to button if oauth service provider is available
        // this prevents users from using the picker if some configuration is missing
        if (isProviderAvailable) {
            // TODO pass existing access token
            String onButtonClick = onClick + ";"
                    + String.format("new nuxeo.utils.GoogleDrivePicker('%s','%s','%s','%s','%s','%s', '%s')",
                            getClientId(), pickId, authId, inputId, infoId, getGoogleDomain(), authorizationUrl);
            writer.writeAttribute("onclick", onButtonClick, null);
        }

        writer.startElement("span", parent);
        writer.writeAttribute("id", pickId, null);
        message = I18NUtils.getMessageString("messages", "label.inputFile.googleDriveUploadPicker", null, locale);
        writer.write(message);
        writer.endElement("span");

        writer.startElement("span", parent);
        writer.writeAttribute("id", authId, null);
        writer.writeAttribute("style", "display:none", null); // hidden
        message = I18NUtils.getMessageString("messages", "label.inputFile.authenticate", null, locale);
        writer.write(message);
        writer.endElement("span");

        writer.endElement("button");

        if (isProviderAvailable) {
            writer.write(ComponentUtils.WHITE_SPACE_CHARACTER);
            writer.startElement("span", parent);
            writer.writeAttribute("id", infoId, null);
            message = I18NUtils.getMessageString("messages", "error.inputFile.noFileSelected", null, locale);
            writer.write(message);
            writer.endElement("span");
        } else {
            // if oauth service provider not properly setup, add warning message
            writer.startElement("span", parent);
            writer.writeAttribute("class", "processMessage completeWarning", null);
            writer.writeAttribute("style", "margin: 0 0 .5em 0; font-size: 11px; background-position-y: 0.6em", null);
            message = I18NUtils.getMessageString("messages", "error.googledrive.providerUnavailable", null, locale);
            writer.write(message);
            writer.endElement("span");
        }

        inputText.setLocalValueSet(false);
        inputText.setStyle("display:none"); // hidden
        ComponentUtils.encodeComponent(context, inputText);
    }

    @Override
    public void validateUpload(UIInput parent, FacesContext context, InputFileInfo submitted) {
        UIComponent facet = parent.getFacet(UPLOAD_GOOGLE_DRIVE_FACET_NAME);
        if (!(facet instanceof HtmlInputText)) {
            return;
        }
        HtmlInputText inputText = (HtmlInputText) facet;
        Object value = inputText.getSubmittedValue();
        if (value != null && !(value instanceof String)) {
            ComponentUtils.addErrorMessage(context, parent, "error.inputFile.invalidSpecialBlob");
            parent.setValid(false);
            return;
        }
        String string = (String) value;
        if (StringUtils.isBlank(string) || string.indexOf(':') < 0) {
            String message = context.getPartialViewContext().isAjaxRequest() ? InputFileInfo.INVALID_WITH_AJAX_MESSAGE
                    : InputFileInfo.INVALID_FILE_MESSAGE;
            ComponentUtils.addErrorMessage(context, parent, message);
            parent.setValid(false);
            return;
        }

        // micro parse the string (user:fileId)
        String[] parts = string.split(":");
        String user = parts[0];
        String fileId = parts[1];

        // check if we can get an access token
        String accessToken = getAccessToken(user);
        if (accessToken == null) {
            String link = String.format(
                    "<a href='#' onclick=\"openPopup('%s'); return false;\">Register a new token</a> and try again.",
                    getOAuthAuthorizationUrl());
            ComponentUtils.addErrorMessage(context, parent, "error.inputFile.accessToken", new Object[] { user, link });
            parent.setValid(false);
            return;
        }

        Blob blob = toBlob(new LiveConnectFileInfo(user, fileId)); // no revisionId
        submitted.setBlob(blob);
        submitted.setFilename(blob.getFilename());
        submitted.setMimeType(blob.getMimeType());
    }

    /**
     * Google Drive upload button is added to the file widget if and only if Google Drive OAuth service provider is
     * enabled
     *
     * @return true if Google Drive OAuth service provider is enabled or false otherwise.
     */
    @Override
    public boolean isEnabled() {
        return getGoogleDriveBlobProvider().getOAuth2Provider().isEnabled();
    }

    /**
     * Creates a Google Drive managed blob.
     *
     * @param fileInfo the Google Drive file info
     * @return the blob
     */
    protected Blob toBlob(LiveConnectFileInfo fileInfo) {
        try {
            return getGoogleDriveBlobProvider().toBlob(fileInfo);
        } catch (IOException e) {
            throw new RuntimeException(e); // TODO better feedback
        }
    }

    protected GoogleDriveBlobProvider getGoogleDriveBlobProvider() {
        return (GoogleDriveBlobProvider) Framework.getService(BlobManager.class).getBlobProvider(id);
    }

    protected String getGoogleDomain() {
        String domain = Framework.getProperty(GOOGLE_DOMAIN_PROP);
        return (domain != null) ? domain : "";
    }

    protected String getClientId() {
        String clientId = getGoogleDriveBlobProvider().getClientId();
        return (clientId != null) ? clientId : "";
    }

    protected String getAccessToken(String user) {
        try {
            Credential credential = getGoogleDriveBlobProvider().getCredential(user);
            if (credential != null) {
                String accessToken = credential.getAccessToken();
                if (accessToken != null) {
                    return accessToken;
                }
            }
        } catch (IOException e) {
            log.error("Failed to get access token for " + user, e);
        }
        return null;
    }

    private boolean hasServiceAccount() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance()
                                                                      .getExternalContext()
                                                                      .getRequest();
        String username = request.getUserPrincipal().getName();
        GoogleOAuth2ServiceProvider provider = getGoogleDriveBlobProvider().getOAuth2Provider();
        return provider != null && provider.getServiceUser(username) != null;
    }

    private String getOAuthAuthorizationUrl() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance()
                                                                      .getExternalContext()
                                                                      .getRequest();
        GoogleOAuth2ServiceProvider provider = getGoogleDriveBlobProvider().getOAuth2Provider();
        return (provider != null && provider.getClientId() != null) ? provider.getAuthorizationUrl(request) : "";
    }
}
