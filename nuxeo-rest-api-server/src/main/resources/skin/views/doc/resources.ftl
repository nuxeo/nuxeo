{
    "apis": [
        {
            "path": "/path.{format}",
            "description": "Access document by their path"
        },
        {
            "path": "/id.{format}",
            "description": "Access document by their id"
        },
        {
            "path": "/automation.{format}",
            "description": "Business object adapter on a document"
        },
        {
            "path": "/user.{format}",
            "description": "Business object adapter on a document"
        },
        {
            "path": "/group.{format}",
            "description": "Business object adapter on a document"
        },
        {
            "path": "/directory.{format}",
            "description": "Business object adapter on a document"
        },
        {
            "path": "/@children.{format}",
            "description": "Access the children of a document"
        },
        {
            "path": "/@search.{format}",
            "description": "Access the children of a document"
        },
        {
            "path": "/@pp.{format}",
            "description": "Access the children of a document"
        },
        {
            "path": "/@acl.{format}",
            "description": "View the acl of a document"
        },
        {
            "path": "/@audit.{format}",
            "description": "View the audit of a document"
        },
        {
            "path": "/@bo.{format}",
            "description": "Business object adapter on a document"
        }
    ],
    "basePath": "${Context.serverURL}${This.path}/",
    "apiVersion": "1.0",
    "swaggerVersion": "1.0"
}