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
 *     Andre Justo
 */
package org.nuxeo.ecm.liveconnect.dropbox;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.ui.web.component.file.InputFileChoice;
import org.nuxeo.ecm.platform.ui.web.component.file.InputFileInfo;
import org.nuxeo.ecm.platform.ui.web.component.file.JSFBlobUploader;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.runtime.api.Framework;

import com.dropbox.core.DbxException;

/**
 * JSF Blob Upload based on Dropbox blobs.
 *
 * @since 7.3
 */
public class DropboxBlobUploader implements JSFBlobUploader {

    public static final String UPLOAD_DROPBOX_FACET_NAME = "uploadDropbox";

    protected final String id;

    public DropboxBlobUploader(String id) {
        this.id = id;
        try {
            getDropboxBlobProvider();
        } catch (NuxeoException e) {
            // this exception is caught by JSFBlobUploaderDescriptor.getJSFBlobUploader
            // to mean that the uploader is not available because badly configured
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getChoice() {
        return InputFileChoice.UPLOAD + "Dropbox";
    }

    @Override
    public void hookSubComponent(UIInput parent) {
        Application app = FacesContext.getCurrentInstance().getApplication();
        ComponentUtils.initiateSubComponent(parent, UPLOAD_DROPBOX_FACET_NAME,
                app.createComponent(HtmlInputText.COMPONENT_TYPE));
    }

    @Override
    public void encodeBeginUpload(UIInput parent, FacesContext context, String onClick) throws IOException {
        UIComponent facet = parent.getFacet(UPLOAD_DROPBOX_FACET_NAME);
        if (!(facet instanceof HtmlInputText)) {
            return;
        }
        HtmlInputText inputText = (HtmlInputText) facet;

        // not ours to close
        @SuppressWarnings("resource")
        ResponseWriter writer = context.getResponseWriter();

        String inputId = facet.getClientId(context);
        String prefix = parent.getClientId(context) + NamingContainer.SEPARATOR_CHAR;
        String pickId = prefix + "DropboxPickMsg";
        String infoId = prefix + "DropboxInfo";
        String authorizationUrl = hasServiceAccount() ? "" : getOAuthAuthorizationUrl();
        Locale locale = context.getViewRoot().getLocale();
        String message;
        boolean isProviderAvailable = getDropboxBlobProvider().getOAuth2Provider().isProviderAvailable();

        writer.startElement("button", parent);
        writer.writeAttribute("type", "button", null);
        writer.writeAttribute("class", "button", null);

        // only add onclick event to button if oauth service provider is available
        // this prevents users from using the picker if some configuration is missing
        if (isProviderAvailable) {
            String onButtonClick = onClick
                    + ";"
                    + String.format("new nuxeo.utils.DropboxPicker('%s', '%s','%s', '%s')", inputId, infoId,
                            authorizationUrl, getClientId());
            writer.writeAttribute("onclick", onButtonClick, null);
        }

        writer.startElement("span", parent);
        writer.writeAttribute("id", pickId, null);
        message = I18NUtils.getMessageString("messages", "label.inputFile.dropboxUploadPicker", null, locale);
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
            writer.writeAttribute(
                    "style",
                    "margin: 0 0 .5em 0; font-size: 11px; padding: 0.4em 0.5em 0.5em 2.2em; background-position-y: 0.6em",
                    null);
            message = I18NUtils.getMessageString("messages", "error.dropbox.providerUnavailable", null, locale);
            writer.write(message);
            writer.endElement("span");
        }

        inputText.setLocalValueSet(false);
        inputText.setStyle("display: none");
        ComponentUtils.encodeComponent(context, inputText);
    }

    @Override
    public void validateUpload(UIInput parent, FacesContext context, InputFileInfo submitted) {
        UIComponent facet = parent.getFacet(UPLOAD_DROPBOX_FACET_NAME);
        if (!(facet instanceof HtmlInputText)) {
            return;
        }
        HtmlInputText inputText = (HtmlInputText) facet;
        Object value = inputText.getSubmittedValue();
        String string;
        if (value == null || value instanceof String) {
            string = (String) value;
        } else {
            ComponentUtils.addErrorMessage(context, parent, "error.inputFile.invalidSpecialBlob");
            parent.setValid(false);
            return;
        }
        if (StringUtils.isBlank(string)) {
            String message = context.getPartialViewContext().isAjaxRequest() ? InputFileInfo.INVALID_WITH_AJAX_MESSAGE
                    : InputFileInfo.INVALID_FILE_MESSAGE;
            ComponentUtils.addErrorMessage(context, parent, message);
            parent.setValid(false);
            return;
        }

        if (getDropboxBlobProvider().getOAuth2Provider() == null) {
            ComponentUtils.addErrorMessage(context, parent, "error.inputFile.dropboxInvalidConfiguration");
            parent.setValid(false);
            return;
        }

        String filePath = getPathFromUrl(string);
        if (StringUtils.isBlank(filePath)) {
            ComponentUtils.addErrorMessage(context, parent, "error.inputFile.invalidFilePath");
            parent.setValid(false);
            return;
        }

        String serviceUserId = getServiceUserId(filePath, FacesContext.getCurrentInstance()
                                                                      .getExternalContext()
                                                                      .getUserPrincipal());
        if (StringUtils.isBlank(serviceUserId)) {
            String link = String.format(
                    "<a href='#' onclick=\"openPopup('%s'); return false;\">Register a new token</a> and try again.",
                    getOAuthAuthorizationUrl());
            ComponentUtils.addErrorMessage(context, parent, "error.inputFile.dropboxInvalidPermissions", new Object[] { link });
            parent.setValid(false);
            return;
        }

        LiveConnectFileInfo fileInfo = new LiveConnectFileInfo(serviceUserId, filePath);
        Blob blob = createBlob(fileInfo);
        submitted.setBlob(blob);
        submitted.setFilename(blob.getFilename());
        submitted.setMimeType(blob.getMimeType());
    }

    /**
     * Dropbox upload button is added to the file widget if and only if Dropbox OAuth service provider is enabled
     *
     * @return true if Dropbox OAuth service provider is enabled or false otherwise.
     */
    @Override
    public boolean isEnabled() {
        return getDropboxBlobProvider().getOAuth2Provider().isEnabled();
    }

    /**
     * Creates a Dropbox managed blob.
     *
     * @param fileInfo the Dropbox file info
     * @return the blob
     */
    protected Blob createBlob(LiveConnectFileInfo fileInfo) {
        try {
            return getDropboxBlobProvider().toBlob(fileInfo);
        } catch (IOException e) {
            throw new RuntimeException(e); // TODO better feedback
        }
    }

    protected String getClientId() {
        String clientId = getDropboxBlobProvider().getClientId();
        return (clientId != null) ? clientId : "";
    }

    protected DropboxBlobProvider getDropboxBlobProvider() {
        return (DropboxBlobProvider) Framework.getService(BlobManager.class).getBlobProvider(id);
    }

    /**
     * Retrieves a file path from a Dropbox sharable URL.
     */
    private String getPathFromUrl(String url) {
        String pattern = "https://dl.dropboxusercontent.com/1/view/[\\w]*";
        String path = url.replaceAll(pattern, "");
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // TODO better feedback
        }
        return path;
    }

    /**
     * Iterates all registered Dropbox tokens of a {@link Principal} to get the serviceLogin of a token with access to a
     * Dropbox file. We need this because Dropbox file picker doesn't provide any information about the account that was
     * used to select the file, and therefore we need to "guess".
     */
    private String getServiceUserId(String filePath, Principal principal) {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("nuxeoLogin", principal.getName());

        DocumentModelList userTokens = getDropboxBlobProvider().getOAuth2Provider()
                                                               .getCredentialDataStore()
                                                               .query(filter);
        for (DocumentModel entry : userTokens) {
            NuxeoOAuth2Token token = new NuxeoOAuth2Token(entry);
            if (hasAccessToFile(filePath, token.getAccessToken())) {
                return token.getServiceLogin();
            }
        }
        return null;
    }

    /**
     * Attempts to retrieve a Dropbox file's metadata to check if an accessToken has permissions to access the file.
     *
     * @return true if metadata was successfully retrieved, or false otherwise.
     */
    private boolean hasAccessToFile(String filePath, String accessToken) {
        try {
            return getDropboxBlobProvider().getDropboxClient(accessToken).files().getMetadata(filePath) != null;
        } catch (DbxException | IOException e) {
            throw new RuntimeException(e); // TODO better feedback
        }
    }

    private boolean hasServiceAccount() {
        HttpServletRequest request = getHttpServletRequest();
        String username = request.getUserPrincipal().getName();
        DropboxOAuth2ServiceProvider provider = getDropboxBlobProvider().getOAuth2Provider();
        return provider != null && provider.getServiceUser(username) != null;
    }

    private String getOAuthAuthorizationUrl() {
        HttpServletRequest request = getHttpServletRequest();
        DropboxOAuth2ServiceProvider provider = getDropboxBlobProvider().getOAuth2Provider();
        return (provider != null && provider.getClientId() != null) ? provider.getAuthorizationUrl(request) : "";
    }

    private HttpServletRequest getHttpServletRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }
}
