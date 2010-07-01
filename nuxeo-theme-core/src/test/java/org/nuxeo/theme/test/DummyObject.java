/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
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

package org.nuxeo.theme.test;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.theme.properties.FieldInfo;

public class DummyObject {

    @FieldInfo
    public String width = "";

    @FieldInfo
    public String height = "";

    @FieldInfo
    public boolean selected = false;

    @FieldInfo
    public Boolean booleanClass = false;

    @FieldInfo
    public int maxItems;

    @FieldInfo
    public Integer integerClass;

    @FieldInfo
    public List<String> stringSequence = new ArrayList<String>();

}
