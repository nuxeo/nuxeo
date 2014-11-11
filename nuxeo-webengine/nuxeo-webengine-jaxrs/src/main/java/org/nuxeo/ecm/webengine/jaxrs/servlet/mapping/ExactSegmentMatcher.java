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


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ExactSegmentMatcher extends SegmentMatcher {

    protected final String pattern;


    public ExactSegmentMatcher(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matches(String segment) {
        return pattern.equals(segment);
    }

    @Override
    public String toString() {
        return pattern;
    }

}
