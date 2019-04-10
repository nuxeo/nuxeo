/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.template.processors;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateProcessor;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

/**
 * Common code between the implementations of {@link TemplateProcessor}
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public abstract class AbstractTemplateProcessor implements TemplateProcessor {

    protected static final int BUFFER_SIZE = 1024 * 64; // 64K

    protected static final Log log = LogFactory.getLog(AbstractTemplateProcessor.class);

    protected File getWorkingDir() {
        String dirPath = System.getProperty("java.io.tmpdir") + "/NXTemplateProcessor" + System.currentTimeMillis();
        File workingDir = new File(dirPath);
        if (workingDir.exists()) {
            FileUtils.deleteTree(workingDir);
        }
        workingDir.mkdir();
        Framework.trackFile(workingDir, workingDir);
        return workingDir;
    }

    protected Blob getSourceTemplateBlob(TemplateBasedDocument templateBasedDocument, String templateName)
            throws Exception {
        Blob sourceTemplateBlob = templateBasedDocument.getTemplateBlob(templateName);

        return sourceTemplateBlob;
    }

}
