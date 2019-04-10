/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *
 */
package org.nuxeo.ecm.platform.audio.extension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.framebody.FrameBodyAPIC;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailFactory;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;

/**
 * Audio thumbnail factory
 *
 * @since 5.7
 */
public class ThumbnailAudioFactory implements ThumbnailFactory {

    private static final Log log = LogFactory.getLog(ThumbnailAudioFactory.class);

    @Override
    public Blob getThumbnail(DocumentModel doc, CoreSession session) {
        if (!doc.hasFacet("Audio")) {
            throw new NuxeoException("Document is not audio type");
        }
        Blob thumbnailBlob = null;
        try {
            if (doc.hasFacet(AudioThumbnailConstants.THUMBNAIL_FACET)) {
                thumbnailBlob = (Blob) doc.getPropertyValue(AudioThumbnailConstants.THUMBNAIL_PROPERTY_NAME);
            }
        } catch (PropertyException e) {
            log.warn("Could not fetch the thumbnail blob", e);
        }
        if (thumbnailBlob == null) {
            TypeInfo docType = doc.getAdapter(TypeInfo.class);
            try {
                return Blobs.createBlob(FileUtils.getResourceFileFromContext("nuxeo.war" + File.separator
                        + docType.getBigIcon()));
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
        }
        return thumbnailBlob;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Blob computeThumbnail(DocumentModel doc, CoreSession session) {
        Blob thumbnailBlob = null;
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        Blob fileBlob;
        try {
            // Get the cover art of the audio file if ID3v2 exist
            try (InputStream in = bh.getBlob().getStream()) {
                fileBlob = Blobs.createBlob(in);
            }
            MP3File file = new MP3File(fileBlob.getFile());
            if (file.hasID3v2Tag()) {
                Iterator it = file.getID3v2Tag().getFrameOfType("APIC");
                if (it != null && it.hasNext()) {
                    AbstractID3v2Frame frame = (AbstractID3v2Frame) it.next();
                    FrameBodyAPIC framePic = (FrameBodyAPIC) frame.getBody();
                    thumbnailBlob = Blobs.createBlob(framePic.getImageData());
                }
            }
        } catch (IOException | TagException | InvalidAudioFrameException | ReadOnlyFileException e) {
            log.warn("Unable to get the audio file cover art", e);
        }
        return thumbnailBlob;
    }
}
