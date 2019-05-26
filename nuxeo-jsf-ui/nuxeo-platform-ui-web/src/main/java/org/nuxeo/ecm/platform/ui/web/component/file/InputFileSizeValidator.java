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
 * $Id: InputFileSizeValidator.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.component.file;

import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;

import com.sun.faces.util.MessageFactory;

/**
 * Input file size validator.
 * <p>
 * Validates an {@link InputFileInfo} blob value in case it's been uploaded. Value is set using the "maxSize" attribute
 * and setting it to (for instance) "10Ko", "10Mo" or "10Go".
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class InputFileSizeValidator implements Validator, StateHolder {

    public static final String VALIDATOR_ID = "InputFileSizeValidator";

    private static final Log log = LogFactory.getLog(InputFileSizeValidator.class);

    private String maxSize = null;

    private boolean maximumSet = false;

    private boolean transientValue = false;

    /**
     * The message identifier of the {@link javax.faces.application.FacesMessage} to be created if the maximum size
     * check fails. The message format string for this message may optionally include the following placeholders:
     * <ul>
     * <li><code>{0}</code> replaced by the configured maximum length.</li>
     * </ul>
     */
    public static final String MAXIMUM_MESSAGE_ID = "error.inputFile.maxSize";

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (!maximumSet) {
            return;
        }
        if (context == null || component == null) {
            throw new IllegalArgumentException();
        }
        if (value != null) {
            if (value instanceof InputFileInfo) {
                InputFileInfo info = (InputFileInfo) value;
                String choice = info.getConvertedChoice();
                if (!(InputFileChoice.isUploadOrKeepTemp(choice))) {
                    return;
                }
                Blob blob = info.getConvertedBlob();
                long finalMaxSize = 0L;
                String maxString = null;
                if (maxSize != null) {
                    finalMaxSize = getMaxSizeBytes();
                    maxString = maxSize;
                }
                if (finalMaxSize != 0L && blob.getLength() > finalMaxSize) {
                    throw new ValidatorException(MessageFactory.getMessage(context, MAXIMUM_MESSAGE_ID, maxString));
                }
            }
        }
    }

    private static long parseMaxSizeString(String maxSize) {
        long res = 0L;
        if (maxSize != null) {
            maxSize = maxSize.trim();
            if (maxSize.length() < 2) {
                log.error("Invalid maximum size " + maxSize);
                return res;
            }
            int maxSizeInt;
            String suffix = maxSize.substring(maxSize.length() - 2);
            String maxSizeIntStr = maxSize.substring(0, maxSize.length() - 2).trim();
            try {
                maxSizeInt = Integer.parseInt(maxSizeIntStr);
            } catch (NumberFormatException e) {
                log.error("Invalid maximum size " + maxSize);
                return res;
            }
            // Using IS units
            if ("Ko".equals(suffix)) {
                res = maxSizeInt * 1000L;
            } else if ("Mo".equals(suffix)) {
                res = maxSizeInt * 1000L * 1000L;
            } else if (maxSize.endsWith("Go")) {
                res = maxSizeInt * 1000L * 1000L * 1000L;
            } else {
                log.error("Invalid maximum size " + maxSize);
            }
        }
        return res;
    }

    public String getMaxSize() {
        return maxSize;
    }

    public long getMaxSizeBytes() {
        return parseMaxSizeString(maxSize);
    }

    public void setMaxSize(String maxSizeString) {
        maxSize = maxSizeString;
        maximumSet = true;
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
        Object[] values = new Object[2];
        values[0] = maxSize;
        values[1] = maximumSet ? Boolean.TRUE : Boolean.FALSE;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        maxSize = (String) values[0];
        maximumSet = (Boolean) values[1];
    }

}
