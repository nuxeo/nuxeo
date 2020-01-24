package org.nuxeo.ecm.platform.comment.impl;

import org.nuxeo.ecm.platform.comment.PropertyCommentFeature;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 11.1
 * @deprecated since 11.1, in order to follow service depreciation
 */
@Deprecated(since = "11.1")
@Features(PropertyCommentFeature.class)
public class PropertyCommentJsonWriterTest extends AbstractCommentJsonWriterTest {

    @Override
    protected Class<? extends CommentManager> getCommentManager() {
        return PropertyCommentManager.class;
    }
}
