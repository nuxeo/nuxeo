/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.osgi;

import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OSGiSystemBundleFile extends OSGiBundleFile {

    public OSGiSystemBundleFile(Path root) throws BundleException {
        super(root);
    }

    @Override
    protected Manifest loadManifest() {
        Manifest mf = new Manifest();
        Attributes attrs = mf.getMainAttributes();
        attrs.putValue(Constants.BUNDLE_SYMBOLICNAME, "org.nuxeo.osgi");
        attrs.putValue(Constants.BUNDLE_NAME, "Nuxeo OSGi Framework Bundle");
        attrs.putValue(Constants.BUNDLE_VENDOR, "Nuxeo");
        attrs.putValue(Constants.BUNDLE_VERSION, "1.0.0");
        attrs.putValue(Constants.BUNDLE_ACTIVATOR, OSGiSystemActivator.class.getName());
        return mf;
    }

}
