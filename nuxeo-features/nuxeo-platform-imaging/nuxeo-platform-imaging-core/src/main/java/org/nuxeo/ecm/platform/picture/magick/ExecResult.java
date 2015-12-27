/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.picture.magick;

import java.util.List;

/**
 * Wraps the exec result of an ImageMagick command line.
 *
 * @author tiry
 * @deprecated Since 5.7.3. Not used, duplicates {@link org.nuxeo.ecm.platform.commandline.executor.api.ExecResult}
 */
@Deprecated
public class ExecResult {

    protected List<String> output;

    protected long execTime;

    public ExecResult(List<String> output, long execTime) {
        this.execTime = execTime;
        this.output = output;
    }

    public List<String> getOutput() {
        return output;
    }

    public long getExecTime() {
        return execTime;
    }

}
