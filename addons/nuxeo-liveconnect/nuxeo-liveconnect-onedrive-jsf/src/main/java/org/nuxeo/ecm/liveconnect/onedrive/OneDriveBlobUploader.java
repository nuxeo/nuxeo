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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.liveconnect.onedrive;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UINamingContainer;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.ui.web.component.file.InputFileChoice;
import org.nuxeo.ecm.platform.ui.web.component.file.InputFileInfo;
import org.nuxeo.ecm.platform.ui.web.component.file.JSFBlobUploader;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.runtime.api.Framework;

import com.google.api.client.auth.oauth2.Credential;

/**
 * JSF Blob Upload based on one drive blobs.
 *
 * @since 8.2
 */
public class OneDriveBlobUploader implements JSFBlobUploader {

    public static final String UPLOAD_ONEDRIVE_FACET_NAME = InputFileChoice.UPLOAD + "OneDrive";

    protected final String id;

    public OneDriveBlobUploader(String id) {
        this.id = id;
        try {
            getOneDriveBlobProvider();
        } catch (NuxeoException e) {
            // this exception is caught by JSFBlobUploaderDescriptor.getJSFBlobUploader
            // to mean that the uploader is not available because badly configured
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getChoice() {
        return UPLOAD_ONEDRIVE_FACET_NAME;
    }

    @Override
    public void hookSubComponent(UIInput parent) {
        Application app = FacesContext.getCurrentInstance().getApplication();
        ComponentUtils.initiateSubComponent(parent, UPLOAD_ONEDRIVE_FACET_NAME,
                app.createComponent(HtmlInputText.COMPONENT_TYPE));
    }

    @Override
    public void encodeBeginUpload(UIInput parent, FacesContext context, String onClick) throws IOException {
        UIComponent facet = parent.getFacet(UPLOAD_ONEDRIVE_FACET_NAME);
        if (!(facet instanceof HtmlInputText)) {
            return;
        }
        HtmlInputText inputText = (HtmlInputText) facet;

        // not ours to close
        @SuppressWarnings("resource")
        ResponseWriter writer = context.getResponseWriter();
        OneDriveBlobProvider blobProvider = getOneDriveBlobProvider();
        OneDriveOAuth2ServiceProvider oauthProvider = blobProvider.getOAuth2Provider();

        String inputId = facet.getClientId(context);
        String prefix = parent.getClientId(context) + UINamingContainer.getSeparatorChar(context);
        String pickId = prefix + "OneDrivePickMsg";
        String infoId = prefix + "OneDriveInfo";
        Locale locale = context.getViewRoot().getLocale();
        String message;
        boolean isProviderAvailable = oauthProvider != null && oauthProvider.isProviderAvailable();

        writer.startElement("button", parent);
        writer.writeAttribute("type", "button", null);
        writer.writeAttribute("class", "button", null);

        // only add onclick event to button if oauth service provider is available
        // this prevents users from using the picker if some configuration is missing
        if (isProviderAvailable) {
            String accessToken = getCurrentUserAccessToken(blobProvider);
            String authorizationUrl = getOAuthAuthorizationUrl(oauthProvider);
            String baseUrl = oauthProvider.getAPIInitializer().apply("").getBaseURL();

            String onButtonClick = onClick + ";"
                    + String.format("new nuxeo.utils.OneDrivePicker('%s', '%s','%s', '%s', '%s', '%s')",
                            getClientId(oauthProvider), inputId, infoId, accessToken, authorizationUrl, baseUrl);
            writer.writeAttribute("onclick", onButtonClick, null);
        }

        writer.startElement("span", parent);
        writer.writeAttribute("id", pickId, null);
        message = I18NUtils.getMessageString("messages", "label.inputFile.oneDriveUploadPicker", null, locale);
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
            writer.writeAttribute("style",
                    "margin: 0 0 .5em 0; font-size: 11px; padding: 0.4em 0.5em 0.5em 2.2em; background-position-y: 0.6em",
                    null);
            message = I18NUtils.getMessageString("messages", "error.oneDrive.providerUnavailable", null, locale);
            writer.write(message);
            writer.endElement("span");
        }

        inputText.setLocalValueSet(false);
        inputText.setStyle("display: none");
        ComponentUtils.encodeComponent(context, inputText);
    }

    @Override
    public void validateUpload(UIInput parent, FacesContext context, InputFileInfo submitted) {
        UIComponent facet = parent.getFacet(UPLOAD_ONEDRIVE_FACET_NAME);
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
        String fileId = (String) value;
        if (StringUtils.isBlank(fileId)) {
            String message = context.getPartialViewContext().isAjaxRequest() ? InputFileInfo.INVALID_WITH_AJAX_MESSAGE
                    : InputFileInfo.INVALID_FILE_MESSAGE;
            ComponentUtils.addErrorMessage(context, parent, message);
            parent.setValid(false);
            return;
        }

        OneDriveBlobProvider blobProvider = getOneDriveBlobProvider();
        OneDriveOAuth2ServiceProvider oauthProvider = blobProvider.getOAuth2Provider();
        if (oauthProvider == null) {
            ComponentUtils.addErrorMessage(context, parent, "error.inputFile.oneDriveInvalidConfiguration");
            parent.setValid(false);
            return;
        }

        Optional<NuxeoOAuth2Token> nuxeoToken = getCurrentNuxeoToken(blobProvider);
        if (!nuxeoToken.isPresent()) {
            String link = String.format(
                    "<a href='#' onclick=\"openPopup('%s'); return false;\">Register a new token</a> and try again.",
                    getOAuthAuthorizationUrl(oauthProvider));
            ComponentUtils.addErrorMessage(context, parent, "error.inputFile.oneDriveInvalidPermissions",
                    new Object[] { link });
            parent.setValid(false);
            return;
        }

        try {
            LiveConnectFileInfo fileInfo = new LiveConnectFileInfo(nuxeoToken.get().getServiceLogin(), fileId);
            Blob blob = blobProvider.toBlob(fileInfo);
            submitted.setBlob(blob);
            submitted.setFilename(blob.getFilename());
            submitted.setMimeType(blob.getMimeType());
        } catch (IOException e) {
            throw new RuntimeException(e); // TODO better feedback
        }
    }

    /**
     * OneDrive upload button is added to the file widget if and only if OneDrive OAuth service provider is enabled
     *
     * @return {@code true} if OneDrive OAuth service provider is enabled or {@code false} otherwise
     */
    @Override
    public boolean isEnabled() {
        OneDriveOAuth2ServiceProvider provider = getOneDriveBlobProvider().getOAuth2Provider();
        return provider != null && provider.isEnabled();
    }

    protected String getClientId(OneDriveOAuth2ServiceProvider provider) {
        return Optional.ofNullable(provider).map(OneDriveOAuth2ServiceProvider::getClientId).orElse("");
    }

    protected OneDriveBlobProvider getOneDriveBlobProvider() {
        return (OneDriveBlobProvider) Framework.getService(BlobManager.class).getBlobProvider(id);
    }

    private String getCurrentUserAccessToken(OneDriveBlobProvider provider) throws IOException {
        Optional<NuxeoOAuth2Token> nuxeoToken = getCurrentNuxeoToken(provider);
        if (nuxeoToken.isPresent()) {
            // Here we don't need to handle NuxeoException as we just retrieved the token
            Credential credential = provider.getCredential(nuxeoToken.get().getServiceLogin());
            Long expiresInSeconds = credential.getExpiresInSeconds();
            if (expiresInSeconds != null && expiresInSeconds > 0) {
                return credential.getAccessToken();
            }
        }
        return "";
    }

    private Optional<NuxeoOAuth2Token> getCurrentNuxeoToken(OneDriveBlobProvider provider) {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put(NuxeoOAuth2Token.KEY_NUXEO_LOGIN,
                FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal().getName());
        return provider.getOAuth2Provider()
                       .getCredentialDataStore()
                       .query(filter)
                       .stream()
                       .map(NuxeoOAuth2Token::new)
                       .findFirst();
    }

    private String getOAuthAuthorizationUrl(OneDriveOAuth2ServiceProvider provider) {
        HttpServletRequest request = getHttpServletRequest();
        return (provider != null && provider.getClientId() != null) ? provider.getAuthorizationUrl(request) : "";
    }

    private HttpServletRequest getHttpServletRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }
}
