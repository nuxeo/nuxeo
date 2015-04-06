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
 *     Nelson Silva
 */
package org.nuxeo.ecm.googleclient;

import java.io.IOException;

import javax.faces.application.Application;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.platform.ui.web.component.file.InputFileChoice;
import org.nuxeo.ecm.platform.ui.web.component.file.InputFileInfo;
import org.nuxeo.ecm.platform.ui.web.component.file.JSFBlobUploader;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * JSF Blob Upload based on Google Drive blobs.
 *
 * @since 7.3
 */
public class GoogleDriveBlobUploader implements JSFBlobUploader {

    public static final String UPLOAD_GOOGLE_DRIVE_FACET_NAME = "uploadGoogleDrive";

    public static final String GOOGLE_API_CLIENT_JS_URL = "https://apis.google.com/js/client.js";

    // ClientId for the file picker auth
    public static final String CLIENT_ID_PROP = "nuxeo.google.clientid";

    // restrict sign-in to accounts at this domain
    public static final String GOOGLE_DOMAIN_PROP = "nuxeo.google.domain";

    protected String clientId;

    public GoogleDriveBlobUploader() {
        clientId = Framework.getProperty(CLIENT_ID_PROP);
        if (StringUtils.isBlank(clientId)) {
            // this exception is caught by JSFBlobUploaderDescriptor.getJSFBlobUploader
            // to mean that the uploader is not available because badly configured
            throw new IllegalStateException("Missing value for property: " + CLIENT_ID_PROP);
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

        writer.startElement("button", parent);
        writer.writeAttribute("type", "button", null);
        writer.writeAttribute("class", "button GoogleDrivePickerButton", null);
        String onButtonClick = onClick
                + ";"
                + String.format("new nuxeo.utils.GoogleDrivePicker('%s','%s','%s','%s','%s','%s')",
            clientId, pickId, authId, inputId, infoId, getGoogleDomain());
        writer.writeAttribute("onclick", onButtonClick, null);

        writer.startElement("span", parent);
        writer.writeAttribute("id", pickId, null);
        writer.write("Google Drive"); // TODO i18n
        writer.endElement("span");

        writer.startElement("span", parent);
        writer.writeAttribute("id", authId, null);
        writer.writeAttribute("style", "display:none", null); // hidden
        writer.write("Click to Authenticate"); // TODO i18n
        writer.endElement("span");

        writer.endElement("button");

        writer.write(ComponentUtils.WHITE_SPACE_CHARACTER);
        writer.startElement("span", parent);
        writer.writeAttribute("id", infoId, null);
        writer.write("no file selected"); // TODO i18n
        writer.endElement("span");

        writer.startElement("script", parent);
        writer.writeAttribute("src", GOOGLE_API_CLIENT_JS_URL, null);
        writer.endElement("script");

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
        Blob blob = createBlob(string);
        submitted.setBlob(blob);
        submitted.setFilename(blob.getFilename());
        submitted.setMimeType(blob.getMimeType());
    }

    /**
     * Creates a Google Drive managed blob.
     *
     * @param fileInfo the Google Drive file info
     * @return the blob
     */
    protected Blob createBlob(String fileInfo) {
        try {
            return getGoogleDriveBlobProvider().getBlob(fileInfo);
        } catch (IOException e) {
            throw new RuntimeException(e); // TODO better feedback
        }
    }

    protected GoogleDriveBlobProvider getGoogleDriveBlobProvider() {
        return (GoogleDriveBlobProvider) Framework.getService(BlobManager.class)
            .getBlobProvider(GoogleDriveComponent.GOOGLE_DRIVE_PREFIX);
    }

    protected String getGoogleDomain() {
        String domain = Framework.getProperty(GOOGLE_DOMAIN_PROP);
        return (domain != null) ? domain : "";
    }
}
