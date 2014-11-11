/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: SelectDataModelListener.java 19474 2007-05-27 10:18:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.model;

import java.util.EventListener;

import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelRowEvent;

/**
 * Listener for select/unselect events happening in a SelectModel.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface SelectDataModelListener extends EventListener {

    void processSelectRowEvent(SelectDataModelRowEvent event);

}
