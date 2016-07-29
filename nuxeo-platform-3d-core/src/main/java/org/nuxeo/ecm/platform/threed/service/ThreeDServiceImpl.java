/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.threed.ThreeD;
import org.nuxeo.ecm.platform.threed.TransmissionThreeD;
import org.nuxeo.runtime.model.DefaultComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link ThreeDService}
 *
 * @since 8.4
 */
public class ThreeDServiceImpl extends DefaultComponent implements ThreeDService {

    protected static final Log log = LogFactory.getLog(ThreeDServiceImpl.class);

    @Override
    public void launchBatchConversion(DocumentModel doc) {
        // XXX implement
    }

    @Override
    public List<Blob> batchConvert(ThreeD originalThreed) {
        List<Blob> blobs = new ArrayList<>();
        // XXX implement
        return blobs;
    }

    @Override
    public TransmissionThreeD convertColladaToglTF(TransmissionThreeD colladaThreeD) {
        TransmissionThreeD gltf = null;
        // XXX implement
        return gltf;
    }
}
