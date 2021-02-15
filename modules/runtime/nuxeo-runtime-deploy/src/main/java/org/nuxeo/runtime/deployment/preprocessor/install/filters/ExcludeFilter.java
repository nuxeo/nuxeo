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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.runtime.deployment.preprocessor.install.filters;

import org.nuxeo.common.utils.Path;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ExcludeFilter extends AbstractFilter {

    public ExcludeFilter(Path pattern) {
        super(pattern);
    }

    public ExcludeFilter(String pattern) {
        super(new Path(pattern));
    }

    @Override
    public boolean accept(Path path) {
        // the default policy is to accept if the path doesn't match to our patterns
        return accept(path, true);
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

}
