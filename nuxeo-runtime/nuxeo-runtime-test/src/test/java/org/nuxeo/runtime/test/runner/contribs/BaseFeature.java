/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.runtime.test.runner.contribs;

import org.junit.Assume;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

public class BaseFeature extends SimpleFeature {

    public BaseFeature() {
        super();
    }

    protected boolean enabled = false;

    public void enable() {
        enabled = true;
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        Assume.assumeTrue(enabled);
    }

}
