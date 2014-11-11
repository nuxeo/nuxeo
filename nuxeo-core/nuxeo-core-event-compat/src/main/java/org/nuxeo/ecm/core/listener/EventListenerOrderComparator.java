/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: EventListenerOrderComparator.java 20632 2007-06-17 10:52:29Z sfermigier $
 */

package org.nuxeo.ecm.core.listener;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator for event listeners using their order setting.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class EventListenerOrderComparator implements Comparator<EventListener>,
        Serializable {

    private static final long serialVersionUID = -3563032494964992219L;

    public int compare(EventListener el1, EventListener el2) {
        int result;
        if (el1 == null) {
            result = el2 == null ? 0 : -1;
        } else if (el2 == null) {
            result = 1;
        } else {
            result = el1.getOrder().compareTo(el2.getOrder());
        }
        return result;
    }

}
