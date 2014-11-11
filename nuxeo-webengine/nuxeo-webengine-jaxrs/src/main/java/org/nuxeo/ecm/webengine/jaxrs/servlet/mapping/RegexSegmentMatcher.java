/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet.mapping;

import java.util.regex.Pattern;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RegexSegmentMatcher extends SegmentMatcher {

    protected final Pattern pattern;

    public RegexSegmentMatcher(String regex) {
        this (Pattern.compile(regex));
    }

    public RegexSegmentMatcher(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matches(String segment) {
        return pattern.matcher(segment).matches();
    }

    @Override
    public String toString() {
        return "("+pattern.toString()+")";
    }
}
