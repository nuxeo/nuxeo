/*
 * (C) Copyright 2010-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tdelprat
 */

package org.nuxeo.connect.client.status;

/**
 * PlaceHolder for Unregistered instances
 */
public class UnresgistedSubscriptionStatusWrapper extends
        SubscriptionStatusWrapper {

    public UnresgistedSubscriptionStatusWrapper() {
        super("Instance is not registered");
    }

    public boolean isConnectServerUnreachable() {
        return false;
    }

}
