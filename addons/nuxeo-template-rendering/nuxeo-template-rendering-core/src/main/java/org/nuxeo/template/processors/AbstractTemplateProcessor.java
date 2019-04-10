/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.template.processors;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.common.Environment;
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
        File workingDir = new File(Environment.getDefault().getTemp(), "NXTemplateProcessor"
                + System.currentTimeMillis());
        if (workingDir.exists()) {
            FileUtils.deleteQuietly(workingDir);
        }
        workingDir.mkdirs();
        Framework.trackFile(workingDir, workingDir);
        return workingDir;
    }

    protected Blob getSourceTemplateBlob(TemplateBasedDocument templateBasedDocument, String templateName) {
        return templateBasedDocument.getTemplateBlob(templateName);
    }

}
