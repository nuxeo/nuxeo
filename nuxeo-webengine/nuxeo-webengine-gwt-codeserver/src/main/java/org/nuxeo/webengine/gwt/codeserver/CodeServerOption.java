/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.webengine.gwt.codeserver;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("option")
public class CodeServerOption {

    @XNode("@name")
    String name;

    @XNode("@value")
    String value;

    void toArgs(List<String> args) {
        args.add(name);
        args.add(value);

        // ensure code server output directories exists
        if (name.endsWith("Dir")) {
            File dir = new File(value);
            FileUtils.deleteQuietly(dir);
            dir.mkdirs();
        }
    }
}
