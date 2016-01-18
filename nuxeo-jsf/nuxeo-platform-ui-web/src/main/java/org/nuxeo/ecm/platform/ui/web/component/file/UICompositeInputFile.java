/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.ui.web.component.file;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.FacesComponent;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UINamingContainer;
import javax.faces.component.html.HtmlInputFile;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.event.ValueChangeEvent;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.platform.ui.web.application.NuxeoResponseStateManagerImpl;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.util.MessageFactory;

/**
 * Backing component for refactor of {@link UIInputFile} using a composite component.
 *
 * @since 8.2
 */
@FacesComponent("org.nuxeo.platform.ui.web.cc.inputFile")
public class UICompositeInputFile extends UIInput implements NamingContainer {

    protected final JSFBlobUploaderService uploaderService;

    protected UIOutputFile outputFile;

    protected enum PropertyKeys {
        choices, checkedChoice, filename, editFilename
    }

    public UICompositeInputFile() {
        uploaderService = Framework.getService(JSFBlobUploaderService.class);
    }

    // Component Basic API

    /**
     * Returns the component family of {@link UINamingContainer}. (that's just required by composite component)
     */
    @Override
    public String getFamily() {
        return UINamingContainer.COMPONENT_FAMILY;
    }

    /**
     * Override value so that an {@link InputFileInfo} structure is given instead of the "value" attribute resolution.
     */
    @Override
    public Object getValue() {
        Object localValue = getLocalValue();
        if (localValue != null) {
            return localValue;
        } else {
            Blob blob = null;
            Object originalValue = super.getValue();
            String mimeType = null;
            if (originalValue instanceof Blob) {
                blob = (Blob) originalValue;
                mimeType = blob.getMimeType();
            }
            List<String> choices = getAvailableChoices(blob, false);
            String choice = choices.get(0);
            return new InputFileInfo(choice, blob, getFilename(), mimeType);
        }
    }

    @Override
    public void decode(FacesContext context) {
        if (context == null) {
            throw new IllegalArgumentException();
        }

        // Force validity back to "true"
        setValid(true);

        // decode the radio button, other input components will decode
        // themselves
        Map<String, String> requestMap = context.getExternalContext().getRequestParameterMap();
        String radioClientId = getClientId(context) + ":choice";
        String choice = requestMap.get(radioClientId);

        InputFileInfo submitted = new InputFileInfo(choice, null, null, null);
        if (InputFileChoice.isKeepOrKeepTemp(choice)) {
            // re-submit already stored values
            InputFileInfo previous = getFileInfoValue();
            if (previous != null) {
                submitted.setBlob(previous.getConvertedBlob());
                submitted.setFilename(previous.getConvertedFilename());
                submitted.setMimeType(previous.getMimeType());
            }
        } else if (InputFileChoice.isUpload(choice)) {
            decodeBlob(context, submitted);
            if (context.getRenderResponse()) {
                // validate phase will not be performed => change choice to "temp keep" directly here
                submitted.setChoice(InputFileChoice.KEEP_TEMP);
            }
        } else if (InputFileChoice.isRemove(choice)) {
            submitted.setBlob(null);
            submitted.setFilename(null);
            submitted.setMimeType(null);
        }

        setSubmittedValue(submitted);
    }

    protected void decodeBlob(FacesContext context, InputFileInfo submitted) {
        UIComponent uploadFacet = getFacet(UIInputFile.UPLOAD_FACET_NAME);
        if (uploadFacet instanceof HtmlInputFile) {
            HtmlInputFile uploadComp = (HtmlInputFile) uploadFacet;
            Object submittedFile = uploadComp.getSubmittedValue();
            if (submittedFile instanceof Blob) {
                Blob sblob = (Blob) submittedFile;
                submitted.setBlob(sblob);
                if (sblob != null) {
                    submitted.setFilename(sblob.getFilename());
                    submitted.setMimeType(sblob.getMimeType());
                }
            } else if (submittedFile == null) {
                // set file to null: blob is null but file is not required
                submitted.setBlob(null);
                submitted.setFilename(null);
                submitted.setMimeType(null);
                submitted.setChoice(InputFileChoice.NONE);
            }
        }
        // TODO: hook for uploader
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        notifyPreviousErrors(context);

        Blob blob = null;
        try {
            blob = getCurrentBlob();
        } catch (ConverterException e) {
            // can happen -> ignore, don't break rendering
        }

        InputFileInfo fileInfo = getFileInfoSubmittedValue();
        if (fileInfo == null) {
            fileInfo = getFileInfoValue();
        }
        String currentChoice = fileInfo.getConvertedChoice();
        boolean temp = InputFileChoice.KEEP_TEMP.equals(currentChoice);
        setCheckedChoice(currentChoice);
        setChoices(getAvailableChoices(blob, temp));

        outputFile.setValue(fileInfo.getConvertedBlob());
        super.encodeBegin(context);
    }

    /**
     * Return specified attribute value or otherwise the specified default if it's null.
     */
    @SuppressWarnings("unchecked")
    protected <T> T getAttributeValue(String key, T defaultValue) {
        T value = (T) getAttributes().get(key);
        return (value != null) ? value : defaultValue;
    }

    private void notifyPreviousErrors(FacesContext context) {
        final Object hasError = context.getAttributes().get(NuxeoResponseStateManagerImpl.MULTIPART_SIZE_ERROR_FLAG);
        final String componentId = (String) context.getAttributes().get(
                NuxeoResponseStateManagerImpl.MULTIPART_SIZE_ERROR_COMPONENT_ID);
        if (Boolean.TRUE.equals(hasError)) {
            if (StringUtils.isBlank(componentId)) {
                ComponentUtils.addErrorMessage(context, this, "error.inputFile.maxRequestSize",
                        new Object[] { Framework.getProperty("nuxeo.jsf.maxRequestSize") });
                // TODO: handle all upload choices
                // } else if (componentId.equals(getFacet(UPLOAD_FACET_NAME).getClientId())) {
                // ComponentUtils.addErrorMessage(context, this, "error.inputFile.maxSize",
                // new Object[] { Framework.getProperty("nuxeo.jsf.maxFileSize") });
            }
        }
    }

    /**
     * Process validation. Sub components are already validated.
     */
    @Override
    public void validate(FacesContext context) {
        if (context == null) {
            throw new IllegalArgumentException();
        }

        // Submitted value == null means "the component was not submitted
        // at all"; validation should not continue
        InputFileInfo submitted = getFileInfoSubmittedValue();
        if (submitted == null) {
            return;
        }
        InputFileInfo toValidate = submitted.clone();

        // validate choice
        String choice;
        try {
            choice = toValidate.getConvertedChoice();
        } catch (ConverterException ce) {
            ComponentUtils.addErrorMessage(context, this, ce.getMessage());
            setValid(false);
            return;
        }
        if (choice == null) {
            ComponentUtils.addErrorMessage(context, this, "error.inputFile.choiceRequired");
            setValid(false);
            return;
        }
        toValidate.setChoice(choice);

        InputFileInfo previous = getPreviousFileInfoValue();
        String previousChoice = previous.getConvertedChoice();
        boolean temp = InputFileChoice.isUploadOrKeepTemp(previousChoice);
        List<String> choices = getAvailableChoices(previous.getBlob(), temp);
        if (!choices.contains(choice)) {
            ComponentUtils.addErrorMessage(context, this, "error.inputFile.invalidChoice");
            setValid(false);
            return;
        }
        if (InputFileChoice.isUpload(previousChoice)) {
            choice = InputFileChoice.UPLOAD;
        }

        // validate choice in respect to other submitted values
        switch (choice) {
        case InputFileChoice.KEEP_TEMP:
            // re-submit stored values
            if (isLocalValueSet()) {
                toValidate.setBlob(previous.getConvertedBlob());
                toValidate.setFilename(previous.getConvertedFilename());
            }
            validateBlob(context, toValidate);
            if (getEditFilename()) {
                validateFilename(context, toValidate);
            }
            break;
        case InputFileChoice.KEEP:
            // re-submit stored values
            toValidate.setBlob(previous.getConvertedBlob());
            toValidate.setFilename(previous.getConvertedFilename());
            validateBlob(context, toValidate);
            if (getEditFilename()) {
                validateFilename(context, toValidate);
            }
            break;
        case InputFileChoice.UPLOAD:
            validateBlob(context, toValidate);
            if (isValid()) {
                toValidate.setChoice(InputFileChoice.KEEP_TEMP);
            }
            break;
        case InputFileChoice.DELETE:
            toValidate.setBlob(null);
            toValidate.setFilename(null);
            break;
        case InputFileChoice.NONE:
            toValidate.setBlob(null);
            toValidate.setFilename(null);
            break;
        }

        if (!isValid()) {
            return;
        }

        // will need this to call declared validators
        super.validateValue(context, toValidate);

        // If our value is valid, store the new value, erase the
        // "submitted" value, and emit a ValueChangeEvent if appropriate
        if (isValid()) {
            setValue(toValidate);
            setSubmittedValue(null);
            if (compareValues(previous, toValidate)) {
                queueEvent(new ValueChangeEvent(this, previous, toValidate));
            }
        }
    }

    protected void handleValidatorException(FacesContext context, ValidatorException ve) {
        setValid(false);
        Collection<FacesMessage> messages = ve.getFacesMessages();
        if (messages != null) {
            for (FacesMessage m : messages) {
                ComponentUtils.addErrorMessage(context, this, m.getSummary());
            }
        } else {
            FacesMessage message = ve.getFacesMessage();
            if (message != null) {
                ComponentUtils.addErrorMessage(context, this, message.getSummary());
            }
        }
    }

    /**
     * Validates submitted blob.
     */
    public void validateBlob(FacesContext context, InputFileInfo submitted) {
        Blob blob = submitted.getConvertedBlob();
        if (blob != null && blob.getLength() == 0) {
            submitted.setBlob(null);
            submitted.setFilename(null);
            submitted.setChoice(InputFileChoice.NONE);
            String message = context.getPartialViewContext().isAjaxRequest() ? InputFileInfo.INVALID_WITH_AJAX_MESSAGE
                    : InputFileInfo.INVALID_FILE_MESSAGE;
            ComponentUtils.addErrorMessage(context, this, message);
            setValid(false);
            return;
        }
    }

    public void validateFilename(FacesContext context, InputFileInfo submitted) {
        // validate filename
        UIComponent filenameFacet = getFacet(UIInputFile.EDIT_FILENAME_FACET_NAME);
        if (filenameFacet instanceof EditableValueHolder) {
            EditableValueHolder filenameComp = (EditableValueHolder) filenameFacet;
            submitted.setFilename(filenameComp.getLocalValue());
            String filename;
            try {
                filename = submitted.getConvertedFilename();
            } catch (ConverterException ce) {
                ComponentUtils.addErrorMessage(context, this, ce.getMessage());
                setValid(false);
                return;
            }
            submitted.setFilename(filename);
        }
    }

    @Override
    public void updateModel(FacesContext context) {
        if (context == null) {
            throw new IllegalArgumentException();
        }

        if (!isValid() || !isLocalValueSet()) {
            return;
        }
        ValueExpression ve = getValueExpression("value");
        if (ve != null) {
            try {
                InputFileInfo local = getFileInfoLocalValue();
                String choice = local.getConvertedChoice();
                // set blob and filename
                if (InputFileChoice.DELETE == choice) {
                    // set filename first to avoid error in case it maps
                    // the blob filename
                    ValueExpression vef = getValueExpression("filename");
                    if (vef != null) {
                        vef.setValue(context.getELContext(), local.getConvertedFilename());
                    }
                    ve.setValue(context.getELContext(), local.getConvertedBlob());
                    setValue(null);
                    setLocalValueSet(false);
                } else if (InputFileChoice.isUploadOrKeepTemp(choice)) {
                    // set blob first to avoid error in case the filename
                    // maps the blob filename
                    ve.setValue(context.getELContext(), local.getConvertedBlob());
                    setValue(null);
                    setLocalValueSet(false);
                    ValueExpression vef = getValueExpression("filename");
                    if (vef != null) {
                        vef.setValue(context.getELContext(), local.getConvertedFilename());
                    }
                } else if (InputFileChoice.KEEP == choice) {
                    // reset local value
                    setValue(null);
                    setLocalValueSet(false);
                    if (getEditFilename()) {
                        // set filename
                        ValueExpression vef = getValueExpression("filename");
                        if (vef != null) {
                            vef.setValue(context.getELContext(), local.getConvertedFilename());
                        }
                    }
                }
                return;
            } catch (ELException e) {
                String messageStr = e.getMessage();
                Throwable result = e.getCause();
                while (null != result && result.getClass().isAssignableFrom(ELException.class)) {
                    messageStr = result.getMessage();
                    result = result.getCause();
                }
                FacesMessage message;
                if (null == messageStr) {
                    message = MessageFactory.getMessage(context, UPDATE_MESSAGE_ID,
                            MessageFactory.getLabel(context, this));
                } else {
                    message = new FacesMessage(FacesMessage.SEVERITY_ERROR, messageStr, messageStr);
                }
                context.addMessage(getClientId(context), message);
                setValid(false);
            } catch (IllegalArgumentException e) {
                FacesMessage message = MessageFactory.getMessage(context, UPDATE_MESSAGE_ID,
                        MessageFactory.getLabel(context, this));
                context.addMessage(getClientId(context), message);
                setValid(false);
            } catch (Exception e) {
                FacesMessage message = MessageFactory.getMessage(context, UPDATE_MESSAGE_ID,
                        MessageFactory.getLabel(context, this));
                context.addMessage(getClientId(context), message);
                setValid(false);
            }
        }
    }

    public void updateFilename(FacesContext context, String newFilename) {
        // set filename by hand after validation
        ValueExpression ve = getValueExpression("filename");
        if (ve != null) {
            ve.setValue(context.getELContext(), newFilename);
        }
    }

    // Component Specific API

    public InputFileInfo getFileInfoLocalValue() {
        return (InputFileInfo) getLocalValue();
    }

    public InputFileInfo getFileInfoSubmittedValue() {
        return (InputFileInfo) getSubmittedValue();
    }

    public InputFileInfo getFileInfoValue() {
        InputFileInfo res = getFileInfoSubmittedValue();
        if (res == null) {
            res = getPreviousFileInfoValue();
        }
        return res;
    }

    public InputFileInfo getPreviousFileInfoValue() {
        InputFileInfo res = getFileInfoLocalValue();
        if (res == null) {
            res = (InputFileInfo) getValue();
        }
        return res;
    }

    public Blob getCurrentBlob() {
        Blob blob = null;
        InputFileInfo ifi = getFileInfoValue();
        if (ifi != null) {
            blob = ifi.getConvertedBlob();
        }
        return blob;
    }

    public String getCurrentFilename() {
        String filename = null;
        InputFileInfo ifi = getFileInfoValue();
        if (ifi != null) {
            filename = ifi.getConvertedFilename();
        }
        return filename;
    }

    protected List<String> getAvailableChoices(Blob blob, boolean temp) {
        List<String> choices = new ArrayList<String>(3);
        boolean isRequired = isRequired();
        if (blob != null) {
            choices.add(temp ? InputFileChoice.KEEP_TEMP : InputFileChoice.KEEP);
        } else if (!isRequired) {
            choices.add(InputFileChoice.NONE);
        }
        boolean allowUpdate = true;
        if (blob != null) {
            BlobManager blobManager = Framework.getService(BlobManager.class);
            BlobProvider blobProvider = blobManager.getBlobProvider(blob);
            if (blobProvider != null && !blobProvider.supportsUserUpdate()) {
                allowUpdate = false;
            }
        }
        if (allowUpdate) {
            for (JSFBlobUploader uploader : uploaderService.getJSFBlobUploaders()) {
                choices.add(uploader.getChoice());
            }
            if (blob != null && !isRequired) {
                choices.add(InputFileChoice.DELETE);
            }
        }
        return choices;
    }

    public String getOptionLabel(String choice) {
        String label = (String) ComponentUtils.getAttributeValue(this, choice + "Label", null);
        if (label == null) {
            label = ComponentUtils.translate(FacesContext.getCurrentInstance(), "label.inputFile." + choice + "Choice");
        }
        return label;
    }

    // Getters/setters

    public UIOutputFile getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(UIOutputFile outputFile) {
        this.outputFile = outputFile;
    }

    public String getFilename() {
        return (String) getStateHelper().eval(PropertyKeys.filename);
    }

    public void setFilename(String filename) {
        getStateHelper().put(PropertyKeys.filename, filename);
    }

    public Boolean getEditFilename() {
        return (Boolean) getStateHelper().eval(PropertyKeys.editFilename, Boolean.FALSE);
    }

    public void setEditFilename(Boolean editFilename) {
        getStateHelper().put(PropertyKeys.editFilename, editFilename);
    }

    @SuppressWarnings("unchecked")
    public List<String> getChoices() {
        return (List<String>) getStateHelper().get(PropertyKeys.choices);
    }

    public void setChoices(List<String> choices) {
        getStateHelper().put(PropertyKeys.choices, choices);
    }

    public String getCheckedChoice() {
        return (String) getStateHelper().get(PropertyKeys.checkedChoice);
    }

    public void setCheckedChoice(String choice) {
        getStateHelper().put(PropertyKeys.checkedChoice, choice);
    }

}
