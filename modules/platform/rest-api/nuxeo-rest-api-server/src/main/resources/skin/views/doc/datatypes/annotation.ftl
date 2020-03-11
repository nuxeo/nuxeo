"annotation" : {
    "id": "annotation",
    "uniqueItems": false,
    "properties": {
        "entity-type": {
            "uniqueItems": false,
            "type": "string",
            "required": true
        },
        "id": {
            "uniqueItems": false,
            "type": "string",
            "required": true
        },
        "documentId": {
            "uniqueItems": false,
            "type": "string",
            "required": true
        },
        "xpath": {
            "uniqueItems": false,
            "type": "string",
            "required": true
        },
        "color": {
            "uniqueItems": false,
            "type": "string",
            "required": false
        },
        "date": {
            "uniqueItems": false,
            "type": "date-time",
            "required": true
        },
        "flags": {
            "uniqueItems": false,
            "type": "string",
            "required": true
        },
        "name": {
            "uniqueItems": false,
            "type": "string",
            "required": true
        },
        "lastModifier": {
            "uniqueItems": false,
            "type": "string",
            "required": true
        },
        "page": {
            "uniqueItems": false,
            "type": "long",
            "required": true
        },
        "position": {
            "uniqueItems": false,
            "type": "string",
            "required": true
        },
        "creationDate": {
            "uniqueItems": false,
            "type": "date-time",
            "required": true
        },
        "opacity": {
            "uniqueItems": false,
            "type": "double",
            "required": true
        },
        "subject": {
            "uniqueItems": false,
            "type": "string",
            "required": true
        },
        "security": {
            "uniqueItems": false,
            "type": "string",
            "required": true
        }
    }
},
"annotationList" : {
    "id": "annotationList",
    "uniqueItems": false,
    "properties": {
        "entity-type": {
            "uniqueItems": false,
            "type": "string",
            "required": true
        },
        "entries": {
            "uniqueItems": false,
            "type": "array",
            "items": {
                "$ref":"annotation"
            },
            "required": true
        }
    }
}