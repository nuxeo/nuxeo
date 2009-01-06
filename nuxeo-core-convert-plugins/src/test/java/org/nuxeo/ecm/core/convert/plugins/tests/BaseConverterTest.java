package org.nuxeo.ecm.core.convert.plugins.tests;

import java.io.File;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public abstract class BaseConverterTest extends NXRuntimeTestCase {

	protected static BlobHolder getBlobFromPath(String path) {
	    File file = FileUtils.getResourceFileFromContext(path);
	    assertTrue(file.length() > 0);
	    return new SimpleBlobHolder(new FileBlob(file));
	}

	public BaseConverterTest() {
		super();
	}

	@Override
	protected void setUp() throws Exception {
	       super.setUp();
	       deployBundle("org.nuxeo.ecm.core.api");
	       deployBundle("org.nuxeo.ecm.core.convert.api");
	       deployBundle("org.nuxeo.ecm.core.convert");
	       deployBundle("org.nuxeo.ecm.core.convert.plugins");
	}

	public BaseConverterTest(String name) {
		super(name);
	}

	protected void doTestTextConverter(String srcMT, String converter, String fileName)
			throws Exception {
			
			    ConversionService cs = Framework.getLocalService(ConversionService.class);
			
			    String converterName = cs.getConverterName(srcMT, "text/plain");
			    assertEquals(converter, converterName);
			
			    BlobHolder hg = getBlobFromPath("test-docs/" + fileName);
			
			    BlobHolder result = cs.convert(converterName, hg, null);
			    assertNotNull(result);
			    assertTrue(result.getBlob().getString().trim().startsWith("Hello"));
			
			    System.out.println(result.getBlob().getString());
			}

	protected void doTestAny2TextConverter(String srcMT, String converterName,
			String fileName) throws Exception {
			
			    ConversionService cs = Framework.getLocalService(ConversionService.class);
			
			    BlobHolder hg = getBlobFromPath("test-docs/" + fileName);
			    hg.getBlob().setMimeType(srcMT);
			
			    BlobHolder result = cs.convert(converterName, hg, null);
			    assertNotNull(result);
			    assertTrue(result.getBlob().getString().trim().startsWith("Hello"));
			
			    System.out.println(result.getBlob().getString());
			}

}