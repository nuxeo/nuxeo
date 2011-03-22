/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     <a href="mailto:stan@nuxeo.com">Sun Seng David TAN</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.operation;

import java.util.Calendar;
import java.util.Date;

/**
 * Perform a ping operation. Can be used in remote client (NxRCP or NxShell) to
 * test connectivity.
 *
 * @author <a href="mailto:stan@nuxeo.com">Sun Seng David TAN</a>
 */
public class PingOperation extends Operation<Date> {

    private static final long serialVersionUID = 1L;

    public PingOperation() {
        super("__PING__");
    }

    @Override
    public Date doRun(ProgressMonitor montior) throws Exception {
        session.save();
        return Calendar.getInstance().getTime();
    }

}
