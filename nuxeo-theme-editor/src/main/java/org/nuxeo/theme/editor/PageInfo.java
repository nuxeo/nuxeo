/*
 * (C) Copyright 2006-2009 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.editor;

public class PageInfo {

    public final String name;

    public final String link;

    public final String className;

    public PageInfo(String name, String link, String className) {
        this.name = name;
        this.link = link;
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public String getLink() {
        return link;
    }

    public String getClassName() {
        return className;
    }

}
