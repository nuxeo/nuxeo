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

package org.nuxeo.runtime.deployment.preprocessor.install;

import java.io.File;
import java.util.HashMap;

import org.nuxeo.common.utils.TextTemplate;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class CommandContextImpl extends HashMap<String, String> implements CommandContext {

    private static final long serialVersionUID = 3020720283855802969L;

    protected final File baseDir;

    public CommandContextImpl(File baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public File getBaseDir() {
        return baseDir;
    }

    @Override
    public String expandVars(String text) {
        return new TextTemplate(this).processText(text);
    }

}
