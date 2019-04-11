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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 7.4
 */
@XObject("variable")
public class SassImport {

    @XNode("@src")
    String src;

    /**
     * Resolved source content
     */
    String content;

    @Override
    public SassImport clone() {
        SassImport clone = new SassImport();
        clone.setSrc(src);
        clone.setContent(content);
        return clone;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SassImport other = (SassImport) obj;
        if (src == null) {
            if (other.src != null)
                return false;
        } else if (!src.equals(other.src))
            return false;
        return true;
    }

    public String getContent() {
        return content;
    }

    public String getSrc() {
        return src;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((src == null) ? 0 : src.hashCode());
        return result;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setSrc(String src) {
        this.src = src;
    }
}
