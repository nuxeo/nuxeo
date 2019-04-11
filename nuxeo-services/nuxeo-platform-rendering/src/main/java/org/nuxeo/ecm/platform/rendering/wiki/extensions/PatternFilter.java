/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.wiki.extensions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.rendering.wiki.WikiFilter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("filter")
public class PatternFilter implements WikiFilter {

    @XNode("pattern")
    protected Pattern pattern;

    @XNode("replacement")
    protected String replacement;

    public PatternFilter(String pattern, String replacement) {
        this.pattern = Pattern.compile(pattern);
        this.replacement = replacement;
    }

    @Override
    public String apply(String content) {
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        do {
            matcher.appendReplacement(sb, replacement);
        } while (matcher.find());
        matcher.appendTail(sb);
        return sb.toString();
    }

}
