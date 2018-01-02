/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: InputFileMimetypeValidator.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.component.file;

import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.faces.util.MessageFactory;

/**
 * Input file mimetype validator.
 * <p>
 * Validates an {@link InputFileInfo} blob value in case it's been uploaded. Accepted mimetypes are set using the
 * "extensions" attribute, representing the list of accepted extension suffixes separated by commas (for instance:
 * ".jpeg, .png").
 * <p>
 * Validation is done on the filename, no actual mimetype check is done for now.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class InputFileMimetypeValidator implements Validator, StateHolder {

    public static final String VALIDATOR_ID = "InputFileMimetypeValidator";

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(InputFileSizeValidator.class);

    private String[] extensions;

    private boolean authorized = true;

    private boolean hidden = false;

    private boolean transientValue = false;

    /**
     * The message identifier of the {@link javax.faces.application.FacesMessage} to be created if the authorized
     * extensions check fails. The message format string for this message may optionally include the following
     * placeholders:
     * <ul>
     * <li><code>{0}</code> replaced by the configured auhtorized extensions.</li>
     * </ul>
     * </p>
     */
    public static final String MIMETYPE_AUTHORIZED_EXTENSIONS_MESSAGE_ID = "error.inputFile.authorizedExtensions";

    /**
     * The message identifier of the {@link javax.faces.application.FacesMessage} to be created if the unauthorized
     * extensions check fails. The message format string for this message may optionally include the following
     * placeholders:
     * <ul>
     * <li><code>{0}</code> replaced by the configured unauthorized extensions.</li>
     * </ul>
     * </p>
     */
    public static final String MIMETYPE_UNAUTHORIZED_EXTENSIONS_MESSAGE_ID = "error.inputFile.unauthorizedExtensions";

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (value != null && extensions != null && extensions.length > 0) {
            if (value instanceof InputFileInfo) {
                InputFileInfo info = (InputFileInfo) value;
                String choice = info.getConvertedChoice();
                if (!InputFileChoice.isUploadOrKeepTemp(choice)) {
                    return;
                }
                String filename = info.getConvertedFilename();
                if (filename != null) {
                    String lowerCaseFilename = filename.toLowerCase();
                    boolean error = authorized;
                    for (String extension : extensions) {
                        String lowerCaseExtension = extension.trim().toLowerCase();
                        if (lowerCaseFilename.endsWith(lowerCaseExtension)) {
                            error = !authorized;
                            break;
                        }
                    }
                    // TODO: handle content types
                    if (error) {
                        String messageId = authorized ? MIMETYPE_AUTHORIZED_EXTENSIONS_MESSAGE_ID
                                : MIMETYPE_UNAUTHORIZED_EXTENSIONS_MESSAGE_ID;
                        throw new ValidatorException(MessageFactory.getMessage(context, messageId,
                                StringUtils.join(extensions, ", ")));
                    }
                }
            }
        }
    }

    public String[] getExtensions() {
        return extensions;
    }

    public void setExtensions(String[] extensions) {
        this.extensions = extensions;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    @Override
    public boolean isTransient() {
        return transientValue;
    }

    @Override
    public void setTransient(boolean newTransientValue) {
        transientValue = newTransientValue;
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[3];
        values[0] = extensions;
        values[1] = authorized;
        values[2] = hidden;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        extensions = (String[]) values[0];
        authorized = ((Boolean) values[1]).booleanValue();
        hidden = ((Boolean) values[2]).booleanValue();
    }

}
