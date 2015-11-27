/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ron1
 */
package org.nuxeo.ecm.platform.rendition.lazy;

import org.nuxeo.ecm.platform.rendition.lazy.AutomationLazyRenditionProvider;

/**
 * @since 7.10
 * @deprecated since 7.10-HF01
 */
@Deprecated
public class AutomationLazyPerUserRenditionProvider extends AutomationLazyRenditionProvider {

    @Override
    protected boolean perUserRendition() {
        return true;
    }

}
