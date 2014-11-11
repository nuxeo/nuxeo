/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: EndAction.java 55400 2008-05-26 09:46:02Z atchertchian $
 */

package org.nuxeo.ecm.platform.mail.listener.action;

import javax.mail.Message;
import javax.mail.Flags.Flag;

import org.nuxeo.ecm.platform.mail.action.ExecutionContext;

/**
*
* @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
*
*/
public class EndAction extends AbstractMailAction {

    public boolean execute(ExecutionContext context) throws Exception {
        try {
            Message message = context.getMessage();
            // erase marker: mail has been treated
            message.setFlag(Flag.FLAGGED, false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void reset(ExecutionContext context) throws Exception {
        // do nothing
    }

}
