/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.functionaltests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriverException;

/**
 * Screenshot into a temp file (will try to save it in maven base dir/target,
 * save it in the system temp folder if can't find it).
 * <p>
 * This temp file won't be deleted on exist.
 *
 * @author Sun Seng David TAN <stan@nuxeo.com>
 */
public class ScreenShotFileOutput implements OutputType<File> {

    private static final Log log = LogFactory.getLog(ScreenShotFileOutput.class);

    protected String targetDir;

    protected String screenshotFilePrefix;

    /**
     * @param targetDir name of the target dir (can be null), will be ignored
     *            if file is created in temp directory.
     * @param screenshotFilePrefix prefix of the screen shot file.
     * @since 5.9.2
     */
    public ScreenShotFileOutput(String targetDir, String screenshotFilePrefix) {
        this.targetDir = targetDir;
        this.screenshotFilePrefix = screenshotFilePrefix;
    }

    /**
     * @param screenshotFilePrefix prefix of the screen shot file.
     */
    public ScreenShotFileOutput(String screenshotFilePrefix) {
        this.screenshotFilePrefix = screenshotFilePrefix;
    }

    @Override
    public File convertFromBase64Png(String base64Png) {
        byte[] data = BYTES.convertFromBase64Png(base64Png);
        return convertFromPngBytes(data);
    }

    @Override
    public File convertFromPngBytes(byte[] data) {
        FileOutputStream fos = null;
        try {
            String location = System.getProperty("basedir") + File.separator
                    + "target";
            File outputFolder = new File(location);
            if (!outputFolder.exists() || !outputFolder.isDirectory()) {
                outputFolder = null;
            }
            if (outputFolder != null && !StringUtils.isBlank(targetDir)) {
                outputFolder = new File(outputFolder, targetDir);
                outputFolder.mkdir();
            }
            File tmpFile = File.createTempFile(screenshotFilePrefix, ".png",
                    outputFolder);
            log.trace(String.format("Created screenshot file named '%s'",
                    tmpFile.getPath()));
            fos = new FileOutputStream(tmpFile);
            fos.write(data);
            return tmpFile;
        } catch (IOException e) {
            throw new WebDriverException(e);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

}
