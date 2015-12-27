/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Max Stepanov
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.domsync.core.events;

/**
 * @author Max Stepanov
 */
public class DOMCharacterDataModifiedEvent extends DOMMutationEvent {

    private static final long serialVersionUID = -5523031456812911018L;

    private final String newValue;

    public DOMCharacterDataModifiedEvent(String target, String newValue) {
        super(target);
        this.newValue = newValue;
    }

    public String getNewValue() {
        return newValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DOMCharacterDataModifiedEvent) {
            return super.equals(obj) && newValue.equals(((DOMCharacterDataModifiedEvent) obj).newValue);
        }
        return false;
    }

    @Override
    public String toString() {
        return "DOMCharacterDataModifiedEvent " + super.toString() + " newValue=" + newValue;
    }
}
