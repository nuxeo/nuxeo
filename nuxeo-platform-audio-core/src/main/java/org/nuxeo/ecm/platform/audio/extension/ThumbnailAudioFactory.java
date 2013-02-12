/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *
 */
package org.nuxeo.ecm.platform.audio.extension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v23Frame;
import org.jaudiotagger.tag.id3.framebody.FrameBodyAPIC;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailFactory;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.runtime.api.Framework;

/**
 * Audio thumbnail factory
 * 
 * @since 5.7
 */
public class ThumbnailAudioFactory implements ThumbnailFactory {

    private static final Log log = LogFactory.getLog(ThumbnailAudioFactory.class);

    @Override
    public Blob getThumbnail(DocumentModel doc, CoreSession session)
            throws ClientException {
        if (!doc.hasFacet("Audio")) {
            throw new ClientException("Document is not audio type");
        }
        Blob thumbnailBlob = null;
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        FileBlob fileBlob;
        try {
            // Get the cover art of the audio file if ID3v2 exist
            fileBlob = new FileBlob(bh.getBlob().getStream());
            MP3File file = new MP3File(fileBlob.getFile());
            if (file.hasID3v2Tag()) {
                ID3v23Frame id3v2 = (ID3v23Frame) file.getID3v2Tag().getFrameOfType(
                        "APIC").next();
                FrameBodyAPIC framePic = (FrameBodyAPIC) id3v2.getBody();
                InputStream is = new ByteArrayInputStream(
                        framePic.getImageData());
                thumbnailBlob = new FileBlob(is);

                // Convert the cover art into thumbnail
                BlobHolder tmpBh = new SimpleBlobHolder(thumbnailBlob);
                ConversionService conversionService = Framework.getLocalService(ConversionService.class);
                tmpBh = conversionService.convert("thumbnailDocumentConverter",
                        tmpBh, null);
                if (tmpBh != null) {
                    thumbnailBlob = tmpBh.getBlob();
                }
            }
        } catch (IOException e) {
            log.warn("Unable to get the audio file cover art", e);
        } catch (TagException e) {
            log.warn("Unable to get the audio file cover art", e);
        } catch (InvalidAudioFrameException e) {
            log.warn("Unable to get the audio file cover art", e);
        } catch (ReadOnlyFileException e) {
            log.warn("Unable to get the audio file cover art", e);
        } finally {
            if (thumbnailBlob == null) {
                TypeInfo docType = doc.getAdapter(TypeInfo.class);
                return new FileBlob(
                        FileUtils.getResourceFileFromContext("nuxeo.war"
                                + File.separator + docType.getBigIcon()));
            }
        }
        return thumbnailBlob;
    }

    @Override
    public Blob computeThumbnail(DocumentModel doc, CoreSession session) {
        return null;
    }
}
