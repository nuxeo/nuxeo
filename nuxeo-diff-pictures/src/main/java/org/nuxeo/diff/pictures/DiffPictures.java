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
 *     thibaud
 */
package org.nuxeo.diff.pictures;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;

/**
 * 
 *
 * @since 7.3
 */
public class DiffPictures {

    private static final Log log = LogFactory.getLog(DiffPictures.class);

    public static final String DEFAULT_COMMAND = "diff-pictures-default";

    public static final String DEFAULT_XPATH = "file:content";

    public static final String DEFAULT_FUZZ = "0";

    public static final String DEFAULT_HIGHLIGHT_COLOR = "Red";

    public static final String DEFAULT_LOWLIGHT_COLOR = "None";

    protected static final String TEMP_DIR_PATH = System.getProperty("java.io.tmpdir");

    Blob b1;

    Blob b2;

    String leftDocId;

    String rightDocId;

    String commandLine;

    Map<String, Serializable> clParameters;

    public DiffPictures(Blob inB1, Blob inB2) {

        this(inB1, inB2, null, null);

    }

    public DiffPictures(DocumentModel inLeft, DocumentModel inRight) {

    	this(inLeft, inRight, null);
    }

    public DiffPictures(DocumentModel inLeft, DocumentModel inRight, String inXPath) {    	
    	
    	Blob leftB, rightB;

    	if(StringUtils.isBlank(inXPath)) {
    		leftB = (Blob) inLeft.getPropertyValue(DEFAULT_XPATH);
    		rightB = (Blob) inRight.getPropertyValue(DEFAULT_XPATH);
    	} else {
    		leftB = (Blob) inLeft.getPropertyValue(inXPath);
    		rightB = (Blob) inRight.getPropertyValue(inXPath);
    	}
    	
    	 init(leftB, rightB, inLeft.getId(), inRight.getId());
    }

    public DiffPictures(Blob inB1, Blob inB2, String inLeftDocId,
            String inRightDocId) {
        init(inB1, inB2, inLeftDocId,
            inRightDocId);

    }
    
    private void init(Blob inB1, Blob inB2, String inLeftDocId,
            String inRightDocId) {

        b1 = inB1;
        b2 = inB2;
        leftDocId = inLeftDocId;
        rightDocId = inRightDocId;
    }

    public Blob compare(String inCommandLine, Map<String, Serializable> inParams)
            throws CommandNotAvailable, IOException {

        Blob result = null;
        String finalName;
        boolean mustTrackTempFile = false;

        commandLine = StringUtils.isBlank(inCommandLine) ? DEFAULT_COMMAND
                : inCommandLine;

        clParameters = inParams == null ? new HashMap<String, Serializable>()
                : inParams;

        finalName = (String) clParameters.get("targetFileName");
        if (StringUtils.isBlank(finalName)) {
            finalName = "comp-" + b1.getFilename();
        }

        // Assume the blob is backed by a File
        CmdParameters params = new CmdParameters();
        String sourceFilePath = b1.getFile().getAbsolutePath();
        params.addNamedParameter("file1", sourceFilePath);

        sourceFilePath = b2.getFile().getAbsolutePath();
        params.addNamedParameter("file2", sourceFilePath);

        checkDefaultParametersValues();
        for (Entry<String, Serializable> entry : clParameters.entrySet()) {
            params.addNamedParameter(entry.getKey(), (String) entry.getValue());
        }

        String destFilePath;

        if (StringUtils.isNotBlank(leftDocId)) {
            File tempFolder = TempFilesHandler.prepareOrGetTempFolder(
                    leftDocId, rightDocId);
            destFilePath = tempFolder.getAbsolutePath() + "/"
                    + System.currentTimeMillis() + "-" + finalName;
        } else {
            mustTrackTempFile = true;
            destFilePath = TEMP_DIR_PATH + "/" + System.currentTimeMillis()
                    + "-" + finalName;
        }

        params.addNamedParameter("targetFilePath", destFilePath);

        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        ExecResult execResult = cles.execCommand(commandLine, params);

        // WARNING
        // ImageMagick can return a non zero code with some of its commands,
        // while the execution went totally OK, with no error. The problem is
        // that the CommandLineExecutorService assumes a non-zero return code is
        // an error => we must handle the thing by ourselves, basically just
        // checking if we do have a comparison file created by ImageMagick
        File tempDestFile = new File(destFilePath);
        if (!tempDestFile.exists()) {
            throw new ClientException("Failed to execute the command <"
                    + commandLine + ">. Final command [ "
                    + execResult.getCommandLine() + " ] returned with error "
                    + execResult.getReturnCode(), execResult.getError());
        } else {
            if (mustTrackTempFile) {
                // Well. If the GC cleans the object and the Framework deletes
                // the file _before_ the browser gets it, then, too bad...
                Framework.trackFile(tempDestFile, this);
            }
        }

        // Framework.trackFile(tempDestFile, this);
        result = new FileBlob(tempDestFile);
        if (result != null) {
            result.setFilename(finalName);
        }

        return result;
    }

    /*
     * Adds the default values if a parameter is missing.
     * 
     * This applies for all command lines (and some will be unused)
     */
    protected void checkDefaultParametersValues() {

        if (clParameters.get("fuzz") == null) {
            clParameters.put("fuzz", DEFAULT_FUZZ);
        }

        if (clParameters.get("highlightColor") == null) {
            clParameters.put("highlightColor", DEFAULT_HIGHLIGHT_COLOR);
        }

        if (clParameters.get("lowlightColor") == null) {
            clParameters.put("lowlightColor", DEFAULT_LOWLIGHT_COLOR);
        }

    }

}
