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

package org.nuxeo.theme.test.fragments;

import junit.framework.TestCase;

import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.fragments.TextFragment;
import org.nuxeo.theme.models.Html;
import org.nuxeo.theme.models.ModelException;

public class TestFragments extends TestCase {

    public void testTextFragment() throws ModelException {
        Fragment fragment = new TextFragment("Text here");
        assertEquals("Text here", ((Html) fragment.getModel()).getBody());
    }

}
