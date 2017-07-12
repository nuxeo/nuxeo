/*
 * (C) Copyright 2012-2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.reload;

/**
 * Copy of event names as triggered by the ReloadService, to make them available on the web layer.
 *
 * @since 5.6
 * @deprecated since 9.3 use directly constants from {@link ReloadService}.
 */
@Deprecated
public class ReloadEventNames {

    public static final String FLUSH_EVENT_ID = ReloadService.FLUSH_EVENT_ID;

    public static final String FLUSH_SEAM_EVENT_ID = ReloadService.FLUSH_SEAM_EVENT_ID;

    public static final String BEFORE_RELOAD_EVENT_ID = ReloadService.BEFORE_RELOAD_EVENT_ID;

    public static final String AFTER_RELOAD_EVENT_ID = ReloadService.AFTER_RELOAD_EVENT_ID;

    public static final String RELOAD_EVENT_ID = ReloadService.RELOAD_EVENT_ID;

    public static final String RELOAD_SEAM_EVENT_ID = ReloadService.RELOAD_SEAM_EVENT_ID;

}
