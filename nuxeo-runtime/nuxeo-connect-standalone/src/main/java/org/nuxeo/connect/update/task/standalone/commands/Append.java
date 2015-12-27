/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.connect.update.task.standalone.commands;

import java.io.File;

/**
 * Append content of a file into a destination file.
 * <p>
 * Rollback command is a Copy of the original file.
 */
public class Append extends Copy {

    @SuppressWarnings("hiding")
    public static final String ID = "append";

    public Append() {
        super(ID);
        overwrite = true;
        append = true;
    }

    public Append(File file, File tofile) {
        super(ID, file, tofile, null, true, false);
        append = true;
    }

}
