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
public abstract class SegmentMatcher {

    public static final SegmentMatcher ANY = new SegmentMatcher() {
        public boolean matches(String segment) { return true; }
        public String toString() { return "**"; }
    };

    public static final SegmentMatcher ANY_SEGMENT = new SegmentMatcher() {
        public boolean matches(String segment) { return true; }
        public String toString() { return "*"; }
    };

    public abstract boolean matches(String segment);


}
