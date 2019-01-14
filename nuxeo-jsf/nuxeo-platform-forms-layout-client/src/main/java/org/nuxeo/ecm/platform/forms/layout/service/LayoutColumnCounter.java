/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.service;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 * Helper component to count number of columns rendered in a layout context.
 * <p>
 * Useful when rendering listing layouts in a PDF table, where number of columns need to be known in advance.
 *
 * @since 5.4.2
 */
// TODO: move this dependency to Seam elsewhere
@Name("layoutColumnCounter")
@Scope(ScopeType.EVENT)
public class LayoutColumnCounter {

    protected Integer index;

    public Integer getCurrentIndex() {
        return index;
    }

    public void setCurrentIndex(Integer index) {
        this.index = index;
    }

    public void increment() {
        if (index == null) {
            index = Integer.valueOf(0);
        }
        this.index = Integer.valueOf(index.intValue() + 1);
    }

    public void decrement() {
        if (index == null) {
            index = Integer.valueOf(0);
        }
        this.index = Integer.valueOf(index.intValue() - 1);
    }

}
