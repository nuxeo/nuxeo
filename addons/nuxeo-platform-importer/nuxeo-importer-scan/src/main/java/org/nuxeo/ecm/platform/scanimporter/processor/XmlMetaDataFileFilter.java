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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.scanimporter.processor;

import java.io.File;
import java.io.FilenameFilter;

/**
 * {@link FilenameFilter} implemetation to skip files that are not descriptors
 *
 * @author Thierry Delprat
 */
public class XmlMetaDataFileFilter implements FilenameFilter {

    @Override
    public boolean accept(File dir, String name) {
        if (name.endsWith(".xml")) {
            return true;
        }
        return false;
    }

}
