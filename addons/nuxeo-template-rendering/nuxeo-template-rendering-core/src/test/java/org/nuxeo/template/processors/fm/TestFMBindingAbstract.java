package org.nuxeo.template.processors.fm;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.types.ComplexTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(FeaturesRunner.class)
@Features({MockitoFeature.class})
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
public abstract class TestFMBindingAbstract {
    protected List<TemplateInput> inputParam;
    protected Map<String, Object> ctx;

    protected FMBindingResolverForTest resolver;

    @Mock
    protected TemplateBasedDocument templateBasedDoc;

    @Mock
    protected DocumentModel doc;

    @Mock
    protected BlobHolder blobHolder;

    @Mock
    protected Property property;

    @Before
    public void setup() {
        resolver = spy(new FMBindingResolverForTest());
        inputParam = new ArrayList<>();
        ctx = new HashMap<>();
        when(templateBasedDoc.getAdaptedDoc()).thenReturn(doc);
    }

    protected void definePropertyInDoc(DocumentModel doc, String xpath, String value) {
        when(property.isScalar()).thenReturn(true);
        definePropertyInDoc(doc, xpath, value, StringType.INSTANCE);
    }

    protected void definePropertyInDoc(DocumentModel doc, String xpath, Blob value) {
        when(doc.getAdapter(BlobHolder.class)).thenReturn(blobHolder);
        when(blobHolder.getBlob()).thenReturn(value);
        Type type = new ComplexTypeImpl(null, "file", "content");
        definePropertyInDoc(doc, xpath, (Serializable) value, type);
    }

    protected void definePropertyInDoc(DocumentModel doc, String xpath, Serializable value, Type type) {
        when(doc.getProperty(xpath)).thenReturn(property);
        when(property.getValue()).thenReturn(value);
        when(property.getType()).thenReturn(type);
        when(doc.getPropertyValue(xpath)).thenReturn(value);
    }
}
