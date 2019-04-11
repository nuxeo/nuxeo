/*
 * (C) Copyright 2010-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tdelprat
 */

package org.nuxeo.connect.client.status;

/**
 * PlaceHolder for Unregistered instances
 */
public class UnresgistedSubscriptionStatusWrapper extends SubscriptionStatusWrapper {

    public UnresgistedSubscriptionStatusWrapper() {
        super("Instance is not registered");
    }

    @Override
    public boolean isConnectServerUnreachable() {
        return false;
    }

}
