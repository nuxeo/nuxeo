/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.importer.stream.consumer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.mp4parser.Box;
import org.mp4parser.Container;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.apple.AppleItemListBox;
import org.mp4parser.boxes.apple.AppleNameBox;
import org.mp4parser.boxes.iso14496.part12.ChunkOffsetBox;
import org.mp4parser.boxes.iso14496.part12.FreeBox;
import org.mp4parser.boxes.iso14496.part12.HandlerBox;
import org.mp4parser.boxes.iso14496.part12.MetaBox;
import org.mp4parser.boxes.iso14496.part12.MovieBox;
import org.mp4parser.boxes.iso14496.part12.UserDataBox;
import org.mp4parser.tools.Path;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.importer.stream.message.BlobMessage;

/**
 * @since 10.1
 */
public class BlobWatermarkMessageConsumer extends BlobMessageConsumer {

    protected final String prefix;

    public BlobWatermarkMessageConsumer(String consumerId, String blobProviderName, BlobInfoWriter blobInfoWriter,
            String watermarkPrefix) {
        super(consumerId, blobProviderName, blobInfoWriter);
        this.prefix = watermarkPrefix;
    }

    @Override
    protected MyBlob getBlob(BlobMessage message) {
        String watermark = getWatermarkString();
        switch (message.getMimetype()) {
        case "text/plain":
            return addWatermarkToText(message, watermark);
        case "image/jpeg":
            return addWatermarkToPicture(message, watermark);
        case "video/mp4":
            return addWatermarkToVideo(message, watermark);
        default:
            return super.getBlob(message);
        }
    }

    protected MyBlob addWatermarkToVideo(BlobMessage message, String watermark) {
        String videoFilePath = message.getPath();
        File videoFile = new File(videoFilePath);
        // videoFile.setReadOnly();
        try {
            IsoFile isoFile = new IsoFile(videoFilePath);
            MovieBox moov = isoFile.getBoxes(MovieBox.class).get(0);
            FreeBox freeBox = findFreeBox(moov);
            long sizeBefore = moov.getSize();
            long offset = 0;
            for (Box box : isoFile.getBoxes()) {
                if ("moov".equals(box.getType())) {
                    break;
                }
                offset += box.getSize();
            }
            boolean correctOffset = needsOffsetCorrection(isoFile);
            // Create structure or just navigate to Apple List Box.
            UserDataBox userDataBox;
            if ((userDataBox = Path.getPath(moov, "udta")) == null) {
                userDataBox = new UserDataBox();
                moov.addBox(userDataBox);
            }
            MetaBox metaBox;
            if ((metaBox = Path.getPath(userDataBox, "meta")) == null) {
                metaBox = new MetaBox();
                HandlerBox hdlr = new HandlerBox();
                hdlr.setHandlerType("mdir");
                metaBox.addBox(hdlr);
                userDataBox.addBox(metaBox);
            }
            AppleItemListBox ilst;
            if ((ilst = Path.getPath(metaBox, "ilst")) == null) {
                ilst = new AppleItemListBox();
                metaBox.addBox(ilst);
            }
            if (freeBox == null) {
                freeBox = new FreeBox(128 * 1024);
                metaBox.addBox(freeBox);
            }
            // Got Apple List Box
            AppleNameBox nam;
            if ((nam = Path.getPath(ilst, "©nam")) == null) {
                nam = new AppleNameBox();
            }
            nam.setDataCountry(0);
            nam.setDataLanguage(0);
            nam.setValue(watermark);
            ilst.addBox(nam);

            long sizeAfter = moov.getSize();
            long diff = sizeAfter - sizeBefore;
            // This is the difference of before/after
            // can we compensate by resizing a Free Box we have found?
            if (freeBox.getData().limit() > diff) {
                // either shrink or grow!
                freeBox.setData(ByteBuffer.allocate((int) (freeBox.getData().limit() - diff)));
                sizeAfter = moov.getSize();
                diff = sizeAfter - sizeBefore;
            }
            if (correctOffset && diff != 0) {
                correctChunkOffsets(moov, diff);
            }
            BetterByteArrayOutputStream baos = new BetterByteArrayOutputStream();
            moov.getBox(Channels.newChannel(baos));
            isoFile.close();

            FileChannel read = new RandomAccessFile(videoFile, "r").getChannel();
            File tmp = File.createTempFile("ChangeMetaData", ".mp4");
            FileChannel write = new RandomAccessFile(tmp, "rw").getChannel();
            if (diff == 0) {
                read.transferTo(0, read.size(), write);
                write.position(offset);
                write.write(ByteBuffer.wrap(baos.getBuffer(), 0, baos.size()));
            } else {
                read.transferTo(0, offset, write);
                write.write(ByteBuffer.wrap(baos.getBuffer(), 0, baos.size()));
                read.transferTo(offset + baos.size(), read.size() - diff, write);
            }
            return new MyBlob(new FileBlob(tmp, message.getMimetype()), tmp.getAbsolutePath());

            // RandomAccessFile f = new RandomAccessFile(videoFilePath, "r");
            // byte[] data = new byte[(int) offset + baos.getBuffer().length];
            // System.out.println("data len " + data.length);
            // f.readFully(data, 0, (int) f.length());
            // ByteBuffer buf = ByteBuffer.wrap(data);
            // buf.position((int) offset);
            // buf.put(baos.getBuffer());
            // return new ByteArrayBlob(data, message.getMimetype());
        } catch (IOException e) {
            throw new IllegalArgumentException("shit happen", e);
        }
    }

    protected boolean needsOffsetCorrection(IsoFile isoFile) {
        if (Path.getPath(isoFile, "moov[0]/mvex[0]") != null) {
            // Fragmented files don't need a correction
            return false;
        } else {
            // no correction needed if mdat is before moov as insert into moov want change the offsets of mdat
            for (Box box : isoFile.getBoxes()) {
                if ("moov".equals(box.getType())) {
                    return true;
                }
                if ("mdat".equals(box.getType())) {
                    return false;
                }
            }
            throw new RuntimeException("I need moov or mdat. Otherwise all this doesn't make sense");
        }
    }

    protected void correctChunkOffsets(MovieBox movieBox, long correction) {
        List<ChunkOffsetBox> chunkOffsetBoxes = Path.getPaths((Box) movieBox, "trak/mdia[0]/minf[0]/stbl[0]/stco[0]");
        if (chunkOffsetBoxes.isEmpty()) {
            chunkOffsetBoxes = Path.getPaths((Box) movieBox, "trak/mdia[0]/minf[0]/stbl[0]/st64[0]");
        }
        for (ChunkOffsetBox chunkOffsetBox : chunkOffsetBoxes) {
            long[] cOffsets = chunkOffsetBox.getChunkOffsets();
            for (int i = 0; i < cOffsets.length; i++) {
                cOffsets[i] += correction;
            }
        }
    }

    protected static class BetterByteArrayOutputStream extends ByteArrayOutputStream {
        byte[] getBuffer() {
            return buf;
        }
    }

    protected FreeBox findFreeBox(Container c) {
        for (Box box : c.getBoxes()) {
            if (box instanceof FreeBox) {
                return (FreeBox) box;
            }
            if (box instanceof Container) {
                FreeBox freeBox = findFreeBox((Container) box);
                if (freeBox != null) {
                    return freeBox;
                }
            }
        }
        return null;
    }

    /**
     * Returns a blob with the jpeg image update the exif software tag with the watermark
     */
    protected MyBlob addWatermarkToPicture(BlobMessage message, String watermark) {
        File jpegImageFile = new File(message.getPath());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            TiffOutputSet outputSet = null;
            ImageMetadata metadata = Imaging.getMetadata(jpegImageFile);
            JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            if (null != jpegMetadata) {
                TiffImageMetadata exif = jpegMetadata.getExif();
                if (null != exif) {
                    outputSet = exif.getOutputSet();
                }
            }
            if (null == outputSet) {
                outputSet = new TiffOutputSet();
            }
            final TiffOutputDirectory exifDirectory = outputSet.getOrCreateRootDirectory();
            exifDirectory.removeField(ExifTagConstants.EXIF_TAG_SOFTWARE);
            exifDirectory.add(ExifTagConstants.EXIF_TAG_SOFTWARE, watermark);
            new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os, outputSet);
            os.flush();
            // FileUtils.writeByteArrayToFile(new File("/tmp/", message.getFilename()), os.toByteArray());
            // System.out.println("done");
            return new MyBlob(new ByteArrayBlob(os.toByteArray(), message.getMimetype()));
        } catch (ImageReadException | ImageWriteException | IOException e) {
            throw new IllegalArgumentException("Unable to edit jpeg " + message, e);
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                // shade
            }
        }
    }

    protected MyBlob addWatermarkToText(BlobMessage message, String watermark) {
        String content = message.getContent();
        if (content == null) {
            try {
                byte[] encoded = Files.readAllBytes(Paths.get(message.getPath()));
                content = new String(encoded, "UTF-8");
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid message: " + message, e);
            }
        }
        return new MyBlob(new StringBlob(watermark + content));
    }

    protected String getWatermarkString() {
        return prefix + " " + System.currentTimeMillis();
    }
}
