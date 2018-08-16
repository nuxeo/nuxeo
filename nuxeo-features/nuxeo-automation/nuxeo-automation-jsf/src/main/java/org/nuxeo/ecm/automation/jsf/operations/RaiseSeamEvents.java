/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
@Operation(id = RaiseSeamEvents.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Raise Seam events", description = "Raise Seam events without parameters. This is a void operation - the input object is returned back as the output", aliases = {
        "WebUI.RaiseSeamEvents" })
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
