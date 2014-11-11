/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.core.redis.retry;

import java.util.concurrent.TimeUnit;

public class ExponentialBackofDelay extends SimpleDelay {

    protected int attempt;

    public ExponentialBackofDelay(int base, int delay) {
        super(base, delay);
    }

    @Override
    protected long computeDelay() {
        int delay =  base * (1 << ++attempt);
        return TimeUnit.MILLISECONDS.convert(delay, TimeUnit.SECONDS);
    }

}
