/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.wiki.extensions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.platform.rendering.wiki.WikiFilter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WikiName implements WikiFilter {

    public static final Pattern pattern = Pattern.compile("[A-Z]+[a-z]+[A-Z][A-Za-z]*");

    public String apply(String content) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.matches())  {
            return "#link#" + content + "#/link#";
        }
        return null;
    }

}
