/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: Functions.java 19475 2007-05-27 10:33:53Z sfermigier $
 */
package org.nuxeo.ecm.platform.ui.web.directory;

/**
 * Chainselect Listbox data. A structure used as var in a jsf 'repeat' iteration.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class CSLData {

    private final int index;

    private final String dirName;

    public CSLData(Integer index, String dirName) {
        this.index = index;
        this.dirName = dirName;
    }

    public int getIndex() {
        return index;
    }

    public String getDirName() {
        return dirName;
    }
}
