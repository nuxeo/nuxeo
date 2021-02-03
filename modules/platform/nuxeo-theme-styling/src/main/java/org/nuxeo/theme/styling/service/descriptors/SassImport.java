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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.theme.styling.service.descriptors;

import org.nuxeo.common.xmap.Resource;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 7.4
 */
@XObject("variable")
public class SassImport {

    @XNode("@src")
    protected String src;

    @XNode("@src")
    protected Resource resource;

    /**
     * Resolved source content
     */
    protected String content;

    public String getContent() {
        return content;
    }

    public String getSrc() {
        return src;
    }

    /** @since 11.5 */
    public Resource getResource() {
        return resource;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
