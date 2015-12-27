/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.ui.web.component.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Service holding the registered JSF Blob Uploaders.
 *
 * @since 7.2
 */
public class JSFBlobUploaderService extends DefaultComponent {

    public static final String XP_UPLOADER = "uploader";

    protected JSFBlobUploaderDescriptorRegistry registry = new JSFBlobUploaderDescriptorRegistry();

    protected static class JSFBlobUploaderDescriptorRegistry extends
            SimpleContributionRegistry<JSFBlobUploaderDescriptor> {

        @Override
        public String getContributionId(JSFBlobUploaderDescriptor contrib) {
            return contrib.id;
        }

        @Override
        public JSFBlobUploaderDescriptor clone(JSFBlobUploaderDescriptor orig) {
            return new JSFBlobUploaderDescriptor(orig);
        }

        @Override
        public void merge(JSFBlobUploaderDescriptor src, JSFBlobUploaderDescriptor dst) {
            dst.merge(src);
        }

        @Override
        public boolean isSupportingMerge() {
            return true;
        }

        public List<JSFBlobUploader> getJSFBlobUploaders() {
            List<JSFBlobUploader> uploaders = new ArrayList<>(currentContribs.size());
            List<JSFBlobUploaderDescriptor> descs = new ArrayList<>(currentContribs.values());
            Collections.sort(descs); // sort according to order
            for (JSFBlobUploaderDescriptor desc : descs) {
                JSFBlobUploader uploader = desc.getJSFBlobUploader();
                if (uploader != null && uploader.isEnabled()) {
                    uploaders.add(uploader);
                }
            }
            return uploaders;
        }

        public JSFBlobUploader getJSFBlobUploader(String choice) {
            for (JSFBlobUploaderDescriptor desc : currentContribs.values()) {
                JSFBlobUploader uploader = desc.getJSFBlobUploader();
                if (uploader != null && uploader.getChoice().equals(choice) && uploader.isEnabled()) {
                    return uploader;
                }
            }
            return null;
        }
    }

    @Override
    public void registerContribution(Object contrib, String xpoint, ComponentInstance contributor) {
        if (XP_UPLOADER.equals(xpoint)) {
            registry.addContribution((JSFBlobUploaderDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String xpoint, ComponentInstance contributor) {
        if (XP_UPLOADER.equals(xpoint)) {
            registry.removeContribution((JSFBlobUploaderDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    public List<JSFBlobUploader> getJSFBlobUploaders() {
        return registry.getJSFBlobUploaders();
    }

    public JSFBlobUploader getJSFBlobUploader(String choice) {
        return registry.getJSFBlobUploader(choice);
    }

}
