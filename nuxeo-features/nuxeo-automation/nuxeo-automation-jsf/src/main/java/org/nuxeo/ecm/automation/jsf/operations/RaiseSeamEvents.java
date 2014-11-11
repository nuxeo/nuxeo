/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.automation.jsf.operations;

import org.jboss.seam.core.Events;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;

/**
 * Operation that raises configurable seam events.
 *
 * @since 5.7
 */
@Operation(id = RaiseSeamEvents.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Raise Seam events", description = "Raise Seam events without parameters. This is a void operation - the input object is returned back as the output")
public class RaiseSeamEvents {

    public static final String ID = "Seam.RaiseEvents";

    @Param(name = "seamEvents", required = true)
    protected StringList seamEvents;

    @OperationMethod
    public void run() {
        if (seamEvents != null) {
            for (String event : seamEvents) {
                Events.instance().raiseEvent(event);
            }
        }
    }

}
