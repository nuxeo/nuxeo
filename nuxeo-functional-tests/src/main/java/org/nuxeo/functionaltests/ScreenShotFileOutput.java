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

import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriverException;

/**
 * Screenshot into a temp file (will try to save it in maven base dir/target,
 * save it in the system temp folder if can't find it). This temp file won't be
 * deleted on exist.
 *
 * @author Sun Seng David TAN <stan@nuxeo.com>
 *
 */
public class ScreenShotFileOutput implements OutputType<File> {

    String screenshotFilePrefix;

    /**
     * @param screenshotFilePrefix prefix of the screen shot file.
     */
    public ScreenShotFileOutput(String screenshotFilePrefix) {
        this.screenshotFilePrefix = screenshotFilePrefix;
    }

    @Override
    public File convertFromBase64Png(String base64Png) {
        FileOutputStream fos = null;
        String location = System.getProperty("basedir") + File.separator
                + "target";

        try {
            byte[] data = BYTES.convertFromBase64Png(base64Png);

            File outputFolder = null;
            if (location != null) {
                outputFolder = new File(location);
                if (!outputFolder.exists() || !outputFolder.isDirectory()) {
                    outputFolder = null;
                }
            }

            File tmpFile = File.createTempFile(screenshotFilePrefix, ".png",
                    outputFolder);

            fos = new FileOutputStream(tmpFile);
            fos.write(data);
            fos.close();
            return tmpFile;
        } catch (IOException e) {
            throw new WebDriverException(e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // Nothing sensible to do
                }
            }
        }
    }
}
