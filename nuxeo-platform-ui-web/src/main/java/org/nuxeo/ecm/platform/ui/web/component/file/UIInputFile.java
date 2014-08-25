/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuxeo.ecm.platform.ui.web.component.file;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlInputFile;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.ConverterException;
import javax.faces.event.ValueChangeEvent;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import javax.servlet.http.Part;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

import com.sun.faces.util.MessageFactory;

/**
 * UIInput file that handles complex validation.
 * <p>
 * Attribute value is the file to be uploaded. Its submitted value as well as
 * filename are handled by sub components. Rendering and validation of
 * subcomponents are handled here.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UIInputFile extends UIInput implements NamingContainer {

    public static final String COMPONENT_TYPE = UIInputFile.class.getName();

    public static final String COMPONENT_FAMILY = "javax.faces.Input";

    protected static final String CHOICE_FACET_NAME = "choice";

    protected static final String UPLOAD_FACET_NAME = "upload";

    protected static final String DEFAULT_DOWNLOAD_FACET_NAME = "default_download";

    protected static final String DOWNLOAD_FACET_NAME = "download";

    protected static final String EDIT_FILENAME_FACET_NAME = "edit_filename";

    protected static final Log log = LogFactory.getLog(UIInputFile.class);

    // value for filename, will disappear when it's part of the blob
    protected String filename;

    // used to decide whether filename can be edited
    protected Boolean editFilename;

    protected String onchange;

    protected String onclick;

    protected String onselect;

    public UIInputFile() {
        // initiate sub components
        FacesContext faces = FacesContext.getCurrentInstance();
        Application app = faces.getApplication();
        ComponentUtils.initiateSubComponent(this, DEFAULT_DOWNLOAD_FACET_NAME,
                app.createComponent(UIOutputFile.COMPONENT_TYPE));
        ComponentUtils.initiateSubComponent(this, EDIT_FILENAME_FACET_NAME,
                app.createComponent(HtmlInputText.COMPONENT_TYPE));
        ComponentUtils.initiateSubComponent(this, UPLOAD_FACET_NAME,
                app.createComponent(HtmlInputFile.COMPONENT_TYPE));
    }

    // component will render itself
    @Override
    public String getRendererType() {
        return null;
    }

    // getters and setters

    /**
     * Override value so that an {@link InputFileInfo} structure is given
     * instead of the "value" attribute resolution.
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
            List<InputFileChoice> choices = getAvailableChoices(blob, false);
            InputFileChoice choice = choices.get(0);
            return new InputFileInfo(choice, blob, getFilename(), mimeType);
        }
    }

    public String getFilename() {
        if (filename != null) {
            return filename;
        }
        ValueExpression ve = getValueExpression("filename");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Boolean getEditFilename() {
        if (editFilename != null) {
            return editFilename;
        }
        ValueExpression ve = getValueExpression("editFilename");
        if (ve != null) {
            try {
                return !Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext()));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return false;
        }
    }

    public void setEditFilename(Boolean editFilename) {
        this.editFilename = editFilename;
    }

    public InputFileInfo getFileInfoValue() {
        return (InputFileInfo) getValue();
    }

    public InputFileInfo getFileInfoLocalValue() {
        return (InputFileInfo) getLocalValue();
    }

    public InputFileInfo getFileInfoSubmittedValue() {
        return (InputFileInfo) getSubmittedValue();
    }

    protected String getStringValue(String name, String defaultValue) {
        ValueExpression ve = getValueExpression(name);
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return defaultValue;
        }
    }

    public String getOnchange() {
        if (onchange != null) {
            return onchange;
        }
        return getStringValue("onchange", null);
    }

    public void setOnchange(String onchange) {
        this.onchange = onchange;
    }

    public String getOnclick() {
        if (onclick != null) {
            return onclick;
        }
        return getStringValue("onclick", null);
    }

    public void setOnclick(String onclick) {
        this.onclick = onclick;
    }

    public String getOnselect() {
        if (onselect != null) {
            return onselect;
        }
        return getStringValue("onselect", null);
    }

    public void setOnselect(String onselect) {
        this.onselect = onselect;
    }

    // handle submitted values

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
        String radioClientId = getClientId(context)
                + NamingContainer.SEPARATOR_CHAR + CHOICE_FACET_NAME;
        String choice = requestMap.get(radioClientId);
        // other submitted values will be handled at validation time
        InputFileInfo submitted = new InputFileInfo(choice, null, null, null);
        setSubmittedValue(submitted);
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

        InputFileInfo previous = getFileInfoValue();

        // validate choice
        InputFileChoice choice;
        try {
            choice = submitted.getConvertedChoice();
        } catch (ConverterException ce) {
            ComponentUtils.addErrorMessage(context, this, ce.getMessage());
            setValid(false);
            return;
        }
        if (choice == null) {
            ComponentUtils.addErrorMessage(context, this,
                    "error.inputFile.choiceRequired");
            setValid(false);
            return;
        }
        submitted.setChoice(choice);
        InputFileChoice previousChoice = previous.getConvertedChoice();
        boolean temp = InputFileChoice.tempKeep == previousChoice
                || InputFileChoice.upload == previousChoice;
        List<InputFileChoice> choices = getAvailableChoices(previous.getBlob(),
                temp);
        if (!choices.contains(choice)) {
            ComponentUtils.addErrorMessage(context, this,
                    "error.inputFile.invalidChoice");
            setValid(false);
            return;
        }

        // validate choice in respect to other submitted values
        switch (choice) {
        case tempKeep:
            // re-submit stored values
            if (isLocalValueSet()) {
                submitted.setBlob(previous.getConvertedBlob());
                submitted.setFilename(previous.getConvertedFilename());
            }
            if (getEditFilename()) {
                validateFilename(context, submitted);
            }
            break;
        case keep:
            // re-submit stored values
            submitted.setBlob(previous.getConvertedBlob());
            submitted.setFilename(previous.getConvertedFilename());
            if (getEditFilename()) {
                validateFilename(context, submitted);
            }
            break;
        case upload:
            try {
                validateBlob(context, submitted);
                if (isValid()) {
                    submitted.setChoice(InputFileChoice.tempKeep);
                }
            } catch (ValidatorException e) {
                // set file to null: blob is null but file is not required
                submitted.setBlob(null);
                submitted.setFilename(null);
                submitted.setChoice(InputFileChoice.none);
            }
            break;
        case delete:
            submitted.setBlob(null);
            submitted.setFilename(null);
            break;
        case none:
            submitted.setBlob(null);
            submitted.setFilename(null);
            break;
        }

        // will need this to call declared validators
        super.validateValue(context, submitted);

        // If our value is valid, store the new value, erase the
        // "submitted" value, and emit a ValueChangeEvent if appropriate
        if (isValid()) {
            setValue(submitted);
            setSubmittedValue(null);
            if (compareValues(previous, submitted)) {
                queueEvent(new ValueChangeEvent(this, previous, submitted));
            }
        }
    }

    public void validateFilename(FacesContext context, InputFileInfo submitted) {
        // validate filename
        UIComponent filenameFacet = getFacet(EDIT_FILENAME_FACET_NAME);
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

    /**
     * Validates submitted blob.
     * <p>
     * Throws ValidatorException as a flag when blob is null and file is not
     * required to set choice back to "no file" (see NXP-1732).
     *
     * @throws ValidatorException
     */
    public void validateBlob(FacesContext context, InputFileInfo submitted)
            throws ValidatorException {
        // validate blob
        UIComponent uploadFacet = getFacet(UPLOAD_FACET_NAME);
        if (uploadFacet instanceof HtmlInputFile) {
            HtmlInputFile uploadComp = (HtmlInputFile) uploadFacet;
            Object submittedFile = uploadComp.getSubmittedValue();
            if (submittedFile instanceof Part) {
                Part file = (Part) submittedFile;
                try {
                    submitted.setBlob(file.getInputStream());
                } catch (IOException e) {
                    ComponentUtils.addErrorMessage(context, this,
                            e.getMessage());
                    setValid(false);
                    return;
                }
                submitted.setFilename(retrieveFilename(file));
                submitted.setMimeType(file.getContentType());
            }
            Blob blob = null;
            try {
                blob = submitted.getConvertedBlob();
            } catch (ConverterException ce) {
                ComponentUtils.addErrorMessage(context, this, ce.getMessage());
                setValid(false);
                return;
            }
            if (blob == null) {
                Map<String, String> requestParameters = context.getExternalContext().getRequestParameterMap();
                String message = requestParameters.containsKey("AJAXREQUEST") ? InputFileInfo.INVALID_WITH_AJAX_MESSAGE
                        : InputFileInfo.INVALID_FILE_MESSAGE;
                ComponentUtils.addErrorMessage(context, this, message);
                setValid(false);
                return;
            }
            // get new filename
            String filename;
            try {
                filename = submitted.getConvertedFilename();
            } catch (ConverterException ce) {
                ComponentUtils.addErrorMessage(context, this, ce.getMessage());
                setValid(false);
                return;
            }
            submitted.setBlob(blob);
            submitted.setFilename(filename);
        }
    }

    // protected method waiting for servlet-api improvements
    protected String retrieveFilename(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                String filename = cd.substring(cd.indexOf('=') + 1).trim().replace(
                        "\"", "");
                return filename;
            }
        }
        return null;
    }

    public void updateFilename(FacesContext context, String newFilename) {
        // set filename by hand after validation
        ValueExpression ve = getValueExpression("filename");
        if (ve != null) {
            ve.setValue(context.getELContext(), newFilename);
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
                InputFileChoice choice = local.getConvertedChoice();
                // set file name
                if ((InputFileChoice.keep == choice && getEditFilename())
                        || InputFileChoice.upload == choice
                        || InputFileChoice.delete == choice
                        || InputFileChoice.tempKeep == choice) {
                }
                // set blob and filename
                if (InputFileChoice.upload == choice
                        || InputFileChoice.delete == choice
                        || InputFileChoice.tempKeep == choice) {
                    if (InputFileChoice.delete == choice) {
                        // set filename first to avoid error in case it maps
                        // the blob filename
                        ValueExpression vef = getValueExpression("filename");
                        if (vef != null) {
                            vef.setValue(context.getELContext(),
                                    local.getConvertedFilename());
                        }
                        ve.setValue(context.getELContext(),
                                local.getConvertedBlob());
                        setValue(null);
                        setLocalValueSet(false);
                    } else {
                        // set blob first to avoid error in case the filename
                        // maps the blob filename
                        ve.setValue(context.getELContext(),
                                local.getConvertedBlob());
                        setValue(null);
                        setLocalValueSet(false);
                        ValueExpression vef = getValueExpression("filename");
                        if (vef != null) {
                            vef.setValue(context.getELContext(),
                                    local.getConvertedFilename());
                        }
                    }
                } else if (InputFileChoice.keep == choice) {
                    // reset local value
                    setValue(null);
                    setLocalValueSet(false);
                    if (getEditFilename()) {
                        // set filename
                        ValueExpression vef = getValueExpression("filename");
                        if (vef != null) {
                            vef.setValue(context.getELContext(),
                                    local.getConvertedFilename());
                        }
                    }
                }
                return;
            } catch (ELException e) {
                String messageStr = e.getMessage();
                Throwable result = e.getCause();
                while (null != result
                        && result.getClass().isAssignableFrom(ELException.class)) {
                    messageStr = result.getMessage();
                    result = result.getCause();
                }
                FacesMessage message;
                if (null == messageStr) {
                    message = MessageFactory.getMessage(context,
                            UPDATE_MESSAGE_ID,
                            MessageFactory.getLabel(context, this));
                } else {
                    message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            messageStr, messageStr);
                }
                context.addMessage(getClientId(context), message);
                setValid(false);
            } catch (IllegalArgumentException e) {
                FacesMessage message = MessageFactory.getMessage(context,
                        UPDATE_MESSAGE_ID,
                        MessageFactory.getLabel(context, this));
                context.addMessage(getClientId(context), message);
                setValid(false);
            } catch (Exception e) {
                FacesMessage message = MessageFactory.getMessage(context,
                        UPDATE_MESSAGE_ID,
                        MessageFactory.getLabel(context, this));
                context.addMessage(getClientId(context), message);
                setValid(false);
            }
        }
    }

    // rendering methods

    protected List<InputFileChoice> getAvailableChoices(Object value,
            boolean temp) {
        List<InputFileChoice> choices = new ArrayList<InputFileChoice>();
        boolean hasFile = value != null;
        boolean isRequired = isRequired();
        if (!hasFile && isRequired) {
            choices.add(InputFileChoice.upload);
        } else if (!hasFile && !isRequired) {
            choices.add(InputFileChoice.none);
            choices.add(InputFileChoice.upload);
        } else if (hasFile && isRequired) {
            if (temp) {
                choices.add(InputFileChoice.tempKeep);
            } else {
                choices.add(InputFileChoice.keep);
            }
            choices.add(InputFileChoice.upload);
        } else if (hasFile && !isRequired) {
            if (temp) {
                choices.add(InputFileChoice.tempKeep);
            } else {
                choices.add(InputFileChoice.keep);
            }
            choices.add(InputFileChoice.upload);
            choices.add(InputFileChoice.delete);
        }
        return choices;
    }

    public Blob getCurrentBlob() {
        Blob blob = null;
        try {
            InputFileInfo submittedFileInfo = getFileInfoSubmittedValue();
            if (submittedFileInfo != null) {
                InputFileChoice choice = submittedFileInfo.getConvertedChoice();
                if (InputFileChoice.keep == choice
                        || InputFileChoice.tempKeep == choice) {
                    // rebuild other info from current value
                    InputFileInfo fileInfo = getFileInfoValue();
                    blob = fileInfo.getConvertedBlob();
                } else {
                    blob = submittedFileInfo.getConvertedBlob();
                }
            } else {
                InputFileInfo fileInfo = getFileInfoValue();
                blob = fileInfo.getConvertedBlob();
            }
        } catch (Exception e) {
            log.error(e);
        }
        return blob;
    }

    public String getCurrentFilename() {
        String filename = null;
        try {
            InputFileInfo submittedFileInfo = getFileInfoSubmittedValue();
            if (submittedFileInfo != null) {
                InputFileChoice choice = submittedFileInfo.getConvertedChoice();
                if (InputFileChoice.keep == choice
                        || InputFileChoice.tempKeep == choice) {
                    // rebuild it in case it's supposed to be kept
                    InputFileInfo fileInfo = getFileInfoValue();
                    filename = fileInfo.getConvertedFilename();
                } else {
                    filename = submittedFileInfo.getConvertedFilename();
                }
            } else {
                InputFileInfo fileInfo = getFileInfoValue();
                filename = fileInfo.getConvertedFilename();
            }
        } catch (Exception e) {
            log.error(e);
        }
        return filename;
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        Blob blob = getCurrentBlob();
        String filename = getCurrentFilename();
        InputFileInfo fileInfo = getFileInfoSubmittedValue();
        if (fileInfo == null) {
            fileInfo = getFileInfoValue();
        }
        InputFileChoice currentChoice = fileInfo.getConvertedChoice();
        boolean temp = InputFileChoice.tempKeep == currentChoice;
        List<InputFileChoice> choices = getAvailableChoices(blob, temp);

        String radioClientId = getClientId(context)
                + NamingContainer.SEPARATOR_CHAR + CHOICE_FACET_NAME;
        writer.startElement("table", this);
        writer.startElement("tbody", this);
        writer.writeAttribute("class", getAttributes().get("styleClass"), null);
        writer.writeAttribute("border", "0", null);
        for (InputFileChoice radioChoice : choices) {
            String id = radioClientId + radioChoice.name();
            writer.startElement("tr", this);
            writer.startElement("td", this);
            writer.writeAttribute("class", "radioColumn", null);
            Map<String, String> props = new HashMap<String, String>();
            props.put("type", "radio");
            props.put("name", radioClientId);
            props.put("id", id);
            props.put("value", radioChoice.name());
            if (radioChoice == currentChoice) {
                props.put("checked", "checked");
            }
            String onchange = getOnchange();
            if (onchange != null) {
                props.put("onchange", onchange);
            }
            String onclick = getOnclick();
            if (onclick != null) {
                props.put("onclick", onclick);
            }
            String onselect = getOnselect();
            if (onselect != null) {
                props.put("onselect", onselect);
            }
            StringBuffer htmlBuffer = new StringBuffer();
            htmlBuffer.append("<input");
            for (Map.Entry<String, String> prop : props.entrySet()) {
                htmlBuffer.append(String.format(" %s=\"%s\"", prop.getKey(),
                        prop.getValue()));
            }
            htmlBuffer.append(" />");
            writer.write(htmlBuffer.toString());
            writer.endElement("td");
            writer.startElement("td", this);
            writer.writeAttribute("class", "fieldColumn", null);
            String html = "<label for=\"%s\" style=\"float:left\">%s</label>";
            String label = (String) ComponentUtils.getAttributeValue(this,
                    radioChoice + "Label", null);
            if (label == null) {
                label = ComponentUtils.translate(context, "label.inputFile."
                        + radioChoice + "Choice");
            }
            writer.write(String.format(html, id, label));
            writer.write(ComponentUtils.WHITE_SPACE_CHARACTER);
            if (InputFileChoice.keep == radioChoice
                    || InputFileChoice.tempKeep == radioChoice) {
                UIComponent downloadFacet = getFacet(DOWNLOAD_FACET_NAME);
                if (downloadFacet != null) {
                    // redefined in template
                    ComponentUtils.encodeComponent(context, downloadFacet);
                } else {
                    downloadFacet = getFacet(DEFAULT_DOWNLOAD_FACET_NAME);
                    if (downloadFacet != null) {
                        UIOutputFile downloadComp = (UIOutputFile) downloadFacet;
                        downloadComp.setQueryParent(true);
                        ComponentUtils.copyValues(
                                this,
                                downloadComp,
                                new String[] { "downloadLabel", "iconRendered" });
                        ComponentUtils.copyLinkValues(this, downloadComp);
                        ComponentUtils.encodeComponent(context, downloadComp);
                    }
                }
                if (getEditFilename()) {
                    writer.write("<br />");
                    UIComponent filenameFacet = getFacet(EDIT_FILENAME_FACET_NAME);
                    if (filenameFacet instanceof HtmlInputText) {
                        HtmlInputText filenameComp = (HtmlInputText) filenameFacet;
                        filenameComp.setValue(filename);
                        filenameComp.setLocalValueSet(false);
                        String onClick = "document.getElementById('%s').checked='checked'";
                        filenameComp.setOnclick(String.format(onClick, id));
                        writer.write(ComponentUtils.WHITE_SPACE_CHARACTER);
                        html = "<label for=\"%s\">%s</label>";
                        label = (String) ComponentUtils.getAttributeValue(this,
                                "editFilenameLabel", null);
                        if (label == null) {
                            label = ComponentUtils.translate(context,
                                    "label.inputFile.editFilename");
                        }
                        writer.write(String.format(html, filenameComp.getId(),
                                label));
                        writer.write(ComponentUtils.WHITE_SPACE_CHARACTER);
                        ComponentUtils.encodeComponent(context, filenameComp);
                    }
                }
            } else if (InputFileChoice.upload == radioChoice) {
                // encode validators info
                long sizeMax = 0L;
                String sizeConstraint = null;
                List<String> authorizedExtensions = new ArrayList<String>();
                List<String> unauthorizedExtensions = new ArrayList<String>();
                boolean hidden = false;
                for (Validator val : getValidators()) {
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
                    constraints.add(ComponentUtils.translate(context,
                            "label.inputFile.maxSize", sizeConstraint));
                }

                if (!hidden
                        && (!authorizedExtensions.isEmpty() || !unauthorizedExtensions.isEmpty())) {
                    if (!authorizedExtensions.isEmpty()) {
                        constraints.add(ComponentUtils.translate(context,
                                "label.inputFile.authorizedExtensions",
                                StringUtils.join(
                                        authorizedExtensions.toArray(), ", ")));
                    }
                    if (!unauthorizedExtensions.isEmpty()) {
                        constraints.add(ComponentUtils.translate(
                                context,
                                "label.inputFile.unauthorizedExtensions",
                                StringUtils.join(
                                        unauthorizedExtensions.toArray(), ", ")));
                    }

                }
                if (constraints.size() > 0) {
                    writer.write("(");
                    writer.write(StringUtils.join(constraints.toArray(), ", "));
                    writer.write(")");
                    writer.write(ComponentUtils.WHITE_SPACE_CHARACTER);
                }
                // encode upload component
                UIComponent uploadFacet = getFacet(UPLOAD_FACET_NAME);
                if (uploadFacet instanceof HtmlInputFile) {
                    HtmlInputFile uploadComp = (HtmlInputFile) uploadFacet;
                    String onClick = "document.getElementById('%s').checked='checked'";
                    uploadComp.setOnfocus(String.format(onClick, id));
                    // TODO: add size limit info
                    ComponentUtils.encodeComponent(context, uploadComp);
                }
            }
            writer.endElement("td");
            writer.endElement("tr");
        }
        writer.endElement("tbody");
        writer.endElement("table");
        writer.flush();
    }

    // state holder

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[6];
        values[0] = super.saveState(context);
        values[1] = filename;
        values[2] = editFilename;
        values[3] = onchange;
        values[4] = onclick;
        values[5] = onselect;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        filename = (String) values[1];
        editFilename = (Boolean) values[2];
        onchange = (String) values[3];
        onclick = (String) values[4];
        onselect = (String) values[5];
    }

}
