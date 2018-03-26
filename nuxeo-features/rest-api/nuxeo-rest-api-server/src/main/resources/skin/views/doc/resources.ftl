{
    "apiVersion": "1.0.0",
    "swaggerVersion": "1.2",
    "apis": [
        {
            "path": "/path.{format}",
            "description": "Access documents by their path"
        },
        {
            "path": "/id.{format}",
            "description": "Access documents by their id"
        },
        {
            "path": "/query.{format}",
            "description": "Document Search"
        },
        {
            "path": "/search.{format}",
            "description": "Search documents and save searches"
        },
        {
            "path": "/blobAdapter.{format}",
            "description": "Get main document blob"
        },
        {
            "path": "/automation.{format}",
            "description": "Run automation operations"
        },
        {
            "path": "/user.{format}",
            "description": "Access users"
        },
        {
            "path": "/group.{format}",
            "description": "Access groups"
        },
        {
            "path": "/me.{format}",
            "description": "Access logged in user"
        },
        {
            "path": "/directory.{format}",
            "description": "Access directories"
        },
        {
            "path": "/childrenAdapter.{format}",
            "description": "Get the children of a document"
        },
        {
            "path": "/searchAdapter.{format}",
            "description": "Search for documents"
        },
        {
            "path": "/ppAdapter.{format}",
            "description": "Execute a page provider"
        },
        {
            "path": "/aclAdapter.{format}",
            "description": "View the acl of a document"
        },
        {
            "path": "/auditAdapter.{format}",
            "description": "View the audit trail of a document"
        },
        {
            "path": "/boAdapter.{format}",
            "description": "Business object adapter on a document"
        },
        {
            "path": "/workflow.{format}",
            "description": "Browse and start workflow instances"
        },
        {
            "path": "/workflowModel.{format}",
            "description": "List workflow models"
        },
        {
            "path": "/task.{format}",
            "description": "Browse and complete task"
        },
        {
            "path": "/facet.{format}",
            "description": "Facet Configurations"
        },
        {
            "path": "/docType.{format}",
            "description": "Document Type Configurations"
        },
        {
            "path": "/schema.{format}",
            "description": "Schema Configurations"
        },
        {
            "path": "/token.{format}",
            "description": "Authentication token"
        },
        {
            "path": "/renditionAdapter.{format}",
            "description": "Rendition on a document"
        },
        {
            "path": "/convertAdapter.{format}",
            "description": "Convert Blobs"
        },
        {
            "path": "/oauth2.{format}",
            "description": "Retrieve OAuth2 authentication data"
        },
        {
            "path": "/emptyDocAdapter.{format}",
            "description": "Initialize an empty document"
        },
        {
            "path": "/annotationAdapter.{format}",
            "description": "Manage annotations on a document"
        }
    ],
    "authorizations": {
        "basicAuth": {
            "type": "basicAuth"
        }
    },
    "basePath": "${Context.serverURL}${This.path}"

}
