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

import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.theme.properties.FieldInfo;

public final class DummyFragment extends AbstractFragment {

    public List<String> getField3() {
        return field3;
    }

    public void setField3(List<String> field3) {
        this.field3 = field3;
    }

    @FieldInfo
    public String field1 = "";

    @FieldInfo
    public String field2 = "";

    @FieldInfo(type = "lines")
    public List<String> field3 = new ArrayList<String>();

    @Override
    public Model getModel() throws ModelException {
        DummyMenu menu = new DummyMenu("Menu", "A menu");
        menu.addItem(new DummyMenuItem("Menu item 1", "A menu item",
                "http://www.some.url.org", true, ""));
        menu.addItem(new DummyMenuItem("Menu item 2", "A menu item",
                "http://www.some.url.org", true, ""));
        return menu;
    }

    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    public String getField2() {
        return field2;
    }

    public void setField2(String field2) {
        this.field2 = field2;
    }

}
