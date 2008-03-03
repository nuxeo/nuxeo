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

import javax.faces.convert.ConverterException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.trinidad.model.UploadedFile;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.ui.web.resolver.TrinidadUploadedFileStreamSource;
import org.nuxeo.runtime.api.Framework;

/**
 * File information used to manage a file adding/removal.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class InputFileInfo {

    private static final Log log = LogFactory.getLog(InputFileInfo.class);

    protected Object choice;

    protected Object blob;

    protected Object filename;

    public InputFileInfo(Object choice, Object blob, Object filename) {
        this.choice = choice;
        this.blob = blob;
        this.filename = filename;
    }

    public Object getBlob() {
        return blob;
    }

    public void setBlob(Object blob) {
        this.blob = blob;
    }

    public Blob getConvertedBlob() {
        Blob convertedBlob = null;
        // XXX AT: later build blob taking care of filename too
        if (blob instanceof Blob) {
            convertedBlob = (Blob) blob;
        } else if (blob instanceof UploadedFile) {
            UploadedFile upFile = (UploadedFile) blob;
            try {
                convertedBlob = createSerializableBlob(upFile, null);
                // TODO: query the service using an adapter
                MimetypeRegistry mimeService = Framework.getService(MimetypeRegistry.class);
                String mimetype = mimeService.getMimetypeFromFilenameAndBlobWithDefault(
                        upFile.getFilename(), convertedBlob, upFile.getContentType());
                convertedBlob.setMimeType(mimetype);
            } catch (MimetypeDetectionException e) {
                throw new ConverterException("error.inputFile.invalidFile");
            } catch (Exception e1) {
                log.error("Error while accessing mimetype service " + e1.getMessage());
                throw new ConverterException("error.inputFile.invalidFile");
            }
        } else if (blob != null) {
            throw new ConverterException("error.inputFile.invalidFile");
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
            convertedFilename = getCleanFilename((String) filename);
        } else if (filename != null) {
            throw new ConverterException("error.inputFile.invalidFilename");
        } else {
            // try to get it from the uploaded file
            if (blob instanceof UploadedFile) {
                String upFilename = ((UploadedFile) blob).getFilename();
                convertedFilename = getCleanFilename(upFilename);
            }
        }
        return convertedFilename;
    }

    protected String getCleanFilename(String filename) {
        // clean file name, fixes NXP-544
        String res = null;
        int lastWinSeparator = filename.lastIndexOf("\\");
        int lastUnixSeparator = filename.lastIndexOf("/");
        int lastSeparator = Math.max(lastWinSeparator, lastUnixSeparator);
        if (lastSeparator != -1) {
            res = filename.substring(lastSeparator + 1, filename.length());
        } else {
            res = filename;
        }
        return res;
    }

    protected static boolean equalValues(Object first, Object second) {
        if (first == null) {
            return second == null;
        } else {
            return first.equals(second);
        }
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
        return true;
    }

    public static Blob createSerializableBlob(UploadedFile file, String mimeType) {
        if (mimeType == null || mimeType.equals("application/octet-stream")) {
            mimeType = file.getContentType();
        }
        TrinidadUploadedFileStreamSource src = new TrinidadUploadedFileStreamSource(
                file);
        return new StreamingBlob(src, mimeType);
    }

}
