/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: InputFileInfo.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.component.file;

import java.io.InputStream;
import java.io.Serializable;

import javax.faces.convert.ConverterException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.ui.web.util.files.FileUtils;

/**
 * File information used to manage a file adding/removal.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class InputFileInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(InputFileInfo.class);

    public static final String EMPTY_FILE_MESSAGE = "error.inputFile.emptyFile";

    public static final String INVALID_FILE_MESSAGE = "error.inputFile.invalidFile";

    public static final String INVALID_WITH_AJAX_MESSAGE = "error.inputFile.ajax";

    protected Object choice;

    protected Object blob;

    protected Object filename;

    protected Object mimeType;

    // empty constructor needed by JSF restore method
    public InputFileInfo() {
        super();
    }

    public InputFileInfo(Object choice, Object blob, Object filename, Object mimeType) {
        this.choice = choice;
        this.blob = blob;
        this.filename = filename;
        this.mimeType = mimeType;
    }

    public Object getBlob() {
        return blob;
    }

    public void setBlob(Object blob) {
        this.blob = blob;
    }

    public Object getMimeType() {
        return mimeType;
    }

    public void setMimeType(Object mimeType) {
        this.mimeType = mimeType;
    }

    public String getConvertedMimeType() throws ConverterException {
        if (mimeType instanceof String) {
            return (String) mimeType;
        } else if (mimeType != null) {
            log.error("Invalid mimetype detected: " + mimeType);
        }
        return null;
    }

    public Blob getConvertedBlob() throws ConverterException {
        Blob convertedBlob = null;
        if (blob instanceof Blob) {
            convertedBlob = (Blob) blob;
        } else if (blob instanceof InputStream) {
            InputStream upFile = (InputStream) blob;
            try {
                if (upFile.available() == 0) {
                    throw new ConverterException(INVALID_FILE_MESSAGE);
                }
                convertedBlob = FileUtils.createSerializableBlob(upFile, getConvertedFilename(),
                        getConvertedMimeType());
            } catch (ConverterException e) {
            } catch (Exception e) {
                throw new ConverterException(INVALID_FILE_MESSAGE);
            }
        } else if (blob != null) {
            throw new ConverterException(INVALID_FILE_MESSAGE);
        }
        return convertedBlob;
    }

    public Object getChoice() {
        return choice;
    }

    public void setChoice(Object choice) {
        this.choice = choice;
    }

    public InputFileChoice getConvertedChoice() throws ConverterException {
        InputFileChoice convertedChoice = null;
        if (choice instanceof InputFileChoice) {
            convertedChoice = (InputFileChoice) choice;
        } else if (choice instanceof String) {
            String stringChoice = (String) choice;
            try {
                convertedChoice = InputFileChoice.valueOf(stringChoice);
            } catch (Exception err) {
                throw new ConverterException("error.inputFile.invalidChoice");
            }
        } else if (choice != null) {
            throw new ConverterException("error.inputFile.invalidChoice");
        }
        return convertedChoice;
    }

    public Object getFilename() {
        return filename;
    }

    public void setFilename(Object filename) {
        this.filename = filename;
    }

    public String getConvertedFilename() throws ConverterException {
        String convertedFilename = null;
        if (filename instanceof String) {
            convertedFilename = FileUtils.getCleanFileName((String) filename);
        } else if (filename != null) {
            throw new ConverterException("error.inputFile.invalidFilename");
        }
        return convertedFilename;
    }

    protected static boolean equalValues(Object first, Object second) {
        if (first == null) {
            return second == null;
        } else {
            return first.equals(second);
        }
    }

    /**
     * Restores information on blob and filename from given {@link InputFileInfo}.
     *
     * @since 10.1
     */
    public void setInfo(InputFileInfo previous) {
        if (previous == null) {
            setBlob(null);
            setFilename(null);
            return;
        }
        setBlob(previous.getConvertedBlob());
        setFilename(previous.getConvertedFilename());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof InputFileInfo)) {
            return false;
        }
        InputFileInfo other = (InputFileInfo) obj;
        if (!equalValues(choice, other.choice)) {
            return false;
        }
        if (!equalValues(filename, other.filename)) {
            return false;
        }
        if (!equalValues(blob, other.blob)) {
            return false;
        }
        if (!equalValues(mimeType, other.mimeType)) {
            return false;
        }
        return true;
    }

    /**
     * @since 6.0-HF17
     */
    public InputFileInfo clone() {
        InputFileInfo clone = new InputFileInfo();
        clone.choice = choice;
        clone.filename = filename;
        clone.blob = blob;
        clone.mimeType = mimeType;
        return clone;
    }

}
