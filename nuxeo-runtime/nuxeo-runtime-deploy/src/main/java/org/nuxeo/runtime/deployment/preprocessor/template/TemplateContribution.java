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

package org.nuxeo.runtime.deployment.preprocessor.template;

import org.nuxeo.common.xmap.Resource;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject
public class TemplateContribution {

    @XNode("@src")
    private Resource src;

    private String template;

    private String marker;

    @XContent
    private String content;

    @XNode("@mode")
    private String mode = "append";

    @XNode("@target")
    public void setTarget(String target) {
        int p = target.lastIndexOf('#');
        if (p > -1) {
            template = target.substring(0, p);
            marker = target.substring(p + 1);
        } else {
            template = target;
            marker = Template.END;
        }
    }

    public String getContent() {
        if (content == null) {
            if (src == null) {
                return "";
            }
            return src.toString();
        }
        return content;
    }

    public String getTemplate() {
        return template;
    }

    public String getMarker() {
        return marker;
    }

    public String getMode() {
        return mode;
    }

    public boolean isAppending() {
        return "append".equals(mode);
    }

    public boolean isPrepending() {
        return "prepend".equals(mode);
    }

    public boolean isReplacing() {
        return "replace".equals(mode);
    }

}
