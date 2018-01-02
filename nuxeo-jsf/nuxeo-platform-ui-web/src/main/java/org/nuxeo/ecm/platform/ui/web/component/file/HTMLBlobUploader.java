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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlInputFile;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.validator.Validator;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * JSF File Upload mechanism based on a standard HTML {@code <input>} field.
 *
 * @since 7.2
 */
public class HTMLBlobUploader implements JSFBlobUploader {

    protected static final String UPLOAD_FACET_NAME = "upload";

    public HTMLBlobUploader(String id) {
    }

    @Override
    public String getChoice() {
        return InputFileChoice.UPLOAD;
    }

    @Override
    public void hookSubComponent(UIInput parent) {
        FacesContext faces = FacesContext.getCurrentInstance();
        Application app = faces.getApplication();
        ComponentUtils.initiateSubComponent(parent, UPLOAD_FACET_NAME,
                app.createComponent(faces, HtmlInputFile.COMPONENT_TYPE, NXFileRenderer.RENDERER_TYPE));
    }

    @Override
    public void encodeBeginUpload(UIInput parent, FacesContext context, String onChange) throws IOException {
        UIComponent facet = parent.getFacet(UPLOAD_FACET_NAME);
        if (!(facet instanceof HtmlInputFile)) {
            return;
        }
        HtmlInputFile inputFile = (HtmlInputFile) facet;

        // not ours to close
        @SuppressWarnings("resource")
        ResponseWriter writer = context.getResponseWriter();

        // encode validators info
        long sizeMax = 0L;
        String sizeConstraint = null;
        List<String> authorizedExtensions = new ArrayList<String>();
        List<String> unauthorizedExtensions = new ArrayList<String>();
        boolean hidden = false;
        for (Validator val : parent.getValidators()) {
            if (val instanceof InputFileSizeValidator) {
                InputFileSizeValidator sizeVal = (InputFileSizeValidator) val;
                long currentSizeMax = sizeVal.getMaxSizeBytes();
                if (currentSizeMax > sizeMax) {
                    sizeMax = currentSizeMax;
                    sizeConstraint = sizeVal.getMaxSize();
                }
            } else if (val instanceof InputFileMimetypeValidator) {
                InputFileMimetypeValidator extVal = (InputFileMimetypeValidator) val;
                hidden = extVal.isHidden();
                if (extVal.isAuthorized()) {
                    authorizedExtensions.addAll(Arrays.asList(extVal.getExtensions()));
                } else {
                    unauthorizedExtensions.addAll(Arrays.asList(extVal.getExtensions()));
                }
            }
        }
        List<String> constraints = new ArrayList<String>();
        if (sizeConstraint != null) {
            constraints.add(ComponentUtils.translate(context, "label.inputFile.maxSize", sizeConstraint));
        }
        if (!hidden && (!authorizedExtensions.isEmpty() || !unauthorizedExtensions.isEmpty())) {
            if (!authorizedExtensions.isEmpty()) {
                constraints.add(ComponentUtils.translate(context, "label.inputFile.authorizedExtensions",
                        StringUtils.join(authorizedExtensions.toArray(), ", ")));
            }
            if (!unauthorizedExtensions.isEmpty()) {
                constraints.add(ComponentUtils.translate(context, "label.inputFile.unauthorizedExtensions",
                        StringUtils.join(unauthorizedExtensions.toArray(), ", ")));
            }
        }
        if (constraints.size() > 0) {
            writer.write("(");
            writer.write(StringUtils.join(constraints.toArray(), ", "));
            writer.write(")");
            writer.write(ComponentUtils.WHITE_SPACE_CHARACTER);
        }

        // encode upload component
        inputFile.setOnchange(onChange);
        // TODO: add size limit info
        ComponentUtils.encodeComponent(context, inputFile);
    }

    @Override
    public void validateUpload(UIInput parent, FacesContext context, InputFileInfo submitted) {
        UIComponent facet = parent.getFacet(UPLOAD_FACET_NAME);
        if (!(facet instanceof HtmlInputFile)) {
            return;
        }
        HtmlInputFile inputFile = (HtmlInputFile) facet;
        Object submittedFile = inputFile.getSubmittedValue();
        if (!(submittedFile instanceof Blob)) {
            return;
        }
        Blob sblob = (Blob) submittedFile;
        if (sblob.getLength() == 0) {
            String message = context.getPartialViewContext().isAjaxRequest() ? InputFileInfo.INVALID_WITH_AJAX_MESSAGE
                    : InputFileInfo.EMPTY_FILE_MESSAGE;
            ComponentUtils.addErrorMessage(context, parent, message);
            parent.setValid(false);
            return;
        }
        submitted.setBlob(sblob);
        submitted.setFilename(sblob.getFilename());
        submitted.setMimeType(sblob.getMimeType());
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
