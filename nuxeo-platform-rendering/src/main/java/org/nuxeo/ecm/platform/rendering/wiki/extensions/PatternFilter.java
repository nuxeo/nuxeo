/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 *
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
