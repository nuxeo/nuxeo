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
