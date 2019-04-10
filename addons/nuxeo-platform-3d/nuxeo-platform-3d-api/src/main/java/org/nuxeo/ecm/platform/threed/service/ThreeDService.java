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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.threed.ThreeD;
import org.nuxeo.ecm.platform.threed.ThreeDBatchProgress;
import org.nuxeo.ecm.platform.threed.TransmissionThreeD;

import java.util.Collection;

/**
 * Service to asynchronously launch and monitor 3D format conversions (including lod) and rendering.
 *
 * @since 8.4
 */
public interface ThreeDService {

    /**
     * Launch all the registered automatic lod transmission version and thumbnail render on the given {@code doc}.
     *
     * @param doc the 3D document to be converted
     */
    void launchBatchConversion(DocumentModel doc);

    /**
     * Batch convert the {@code originalThreed} to all needed blobs (lod transmission formats and thumbnail render)
     *
     * @param originalThreed the 3d to convert
     * @return a {@code BlobHolder} object of the converted assets.
     */
    BlobHolder batchConvert(ThreeD originalThreed);

    /**
     * Clears data model for render views and transmission formats.
     */
    void cleanBatchData(DocumentModel doc);

    /**
     * Batch convert the Collada {@code colladaThreeD} to glTF
     *
     * @param colladaThreeD the 3d to convert
     * @return a {@code TransmissionThreeD} object of in glTF.
     */
    TransmissionThreeD convertColladaToglTF(TransmissionThreeD colladaThreeD);

    /**
     * Returns the available registered render views on a 3D content.
     */
    Collection<RenderView> getAvailableRenderViews();

    /**
     * Returns the automatic registered render views on a 3D content.
     */
    Collection<RenderView> getAutomaticRenderViews();

    /**
     * Returns the available registered automatic LODs on a 3D content.
     */
    Collection<AutomaticLOD> getAvailableLODs();

    /**
     * Returns the automatic registered automatic LODs on a 3D content.
     */
    Collection<AutomaticLOD> getAutomaticLODs();

    /**
     * Returns the available registered Automatic LOD by id.
     */
    AutomaticLOD getAutomaticLOD(String id);

    /**
     * Returns the available registered render views by id.
     */
    RenderView getRenderView(String id);

    /**
     * Returns the available registered render views by azimuth and zenith (the combination is always unique).
     */
    RenderView getRenderView(Integer azimuth, Integer Zenith);

    /**
     * Get the batch processing progress
     *
     * @param repositoryName
     * @param docId of the document being processed
     * @return a {@link ThreeDBatchProgress} with status (queued, running, unknown) and a message of the running state
     */
    ThreeDBatchProgress getBatchProgress(String repositoryName, String docId);
}
