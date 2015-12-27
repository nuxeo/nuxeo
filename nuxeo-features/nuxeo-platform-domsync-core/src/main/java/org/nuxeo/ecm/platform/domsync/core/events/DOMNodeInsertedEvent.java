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
public class DOMNodeInsertedEvent extends DOMMutationEvent {

    private static final long serialVersionUID = 2062844930910763189L;

    private final int position;

    private final String fragment;

    public DOMNodeInsertedEvent(String target, String fragment, int position) {
        super(target);
        this.fragment = fragment;
        this.position = position;
    }

    public String getFragment() {
        return fragment;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DOMNodeInsertedEvent) {
            DOMNodeInsertedEvent other = (DOMNodeInsertedEvent) obj;
            return super.equals(obj) && position == other.position && fragment.equals(other.fragment);
        }
        return false;
    }

    @Override
    public String toString() {
        return "DOMNodeInsertedEvent " + super.toString() + " position=" + position + " fragment=" + fragment;
    }

}
