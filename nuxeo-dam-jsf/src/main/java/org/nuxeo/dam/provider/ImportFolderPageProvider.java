package org.nuxeo.dam.provider;

import static org.nuxeo.dam.DamConstants.ASSET_FACET;

import java.util.Collection;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.2
 */
public class ImportFolderPageProvider extends CoreQueryDocumentPageProvider {

    protected static final WritePermissionFilter WRITE_PERMISSION_FILTER = new WritePermissionFilter();

    protected static final AssetSubTypeFilter ASSET_SUB_TYPE_FILTER = new AssetSubTypeFilter();

    @Override
    protected Filter getFilter() {
        return new CompoundFilter(WRITE_PERMISSION_FILTER,
                ASSET_SUB_TYPE_FILTER);
    }

    protected static class WritePermissionFilter implements Filter {

        @Override
        public boolean accept(DocumentModel doc) {
            CoreSession session = doc.getCoreSession();
            if (session == null) {
                return false;
            }
            try {
                return session.hasPermission(doc.getRef(),
                        SecurityConstants.WRITE);
            } catch (ClientException e) {
                return false;
            }
        }
    }

    protected static class AssetSubTypeFilter implements Filter {

        @Override
        public boolean accept(DocumentModel doc) {
            SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
            TypeManager typeManager = Framework.getLocalService(TypeManager.class);
            Collection<Type> allowedSubTypes = typeManager.getAllowedSubTypes(
                    doc.getType(), doc);
            for (Type type : allowedSubTypes) {
                DocumentType docType = schemaManager.getDocumentType(type.getId());
                if (docType.hasFacet(ASSET_FACET)) {
                    return true;
                }
            }
            return false;
        }

    }

}
