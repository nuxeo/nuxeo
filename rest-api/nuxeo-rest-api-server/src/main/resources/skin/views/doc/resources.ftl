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
        }
    ],
    "authorizations": {
        "basicAuth": {
            "type": "basicAuth"
        }
    },
    "basePath": "${Context.serverURL}${This.path}"

}