package org.nuxeo.importer.stream;

import org.nuxeo.importer.stream.message.BlobInfoMessage;
import org.nuxeo.importer.stream.message.BlobMessage;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;

/**
 * Helper class to define different log names used in nuxeo-importer-stream and utility methods to get {@link Codec
 * codecs}.
 *
 * @since 11.1
 */
public final class StreamImporters {

    // use java codec because it was already used with kafka and because avro has difficulties to serialize BlobMessage
    // due to Blob interface. Using codec allow us to not use chronicle serialization which could be limited
    public static final String DEFAULT_CODEC = "java";

    public static final String DEFAULT_LOG_CONFIG = "default";

    public static final String DEFAULT_LOG_BLOB_NAME = "import/blob";

    public static final String DEFAULT_LOG_BLOB_INFO_NAME = "import/blob-info";

    public static final String DEFAULT_LOG_DOC_NAME = "import/doc";

    public static Codec<BlobMessage> getBlobCodec() {
        return Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, BlobMessage.class);
    }

    public static Codec<BlobInfoMessage> getBlobInfoCodec() {
        return Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, BlobInfoMessage.class);
    }

    public static Codec<DocumentMessage> getDocCodec() {
        return Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, DocumentMessage.class);
    }

    private StreamImporters() {
        // not allowed
    }
}
