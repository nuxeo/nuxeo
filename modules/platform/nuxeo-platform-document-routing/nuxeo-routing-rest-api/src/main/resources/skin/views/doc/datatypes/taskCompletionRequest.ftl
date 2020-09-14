"taskCompletionRequest" : {
    "id": "taskCompletionRequest",
    "type": "any",
    "required": false,
    "uniqueItems": false,
    "properties": {
        "entity-type": {
            "uniqueItems": false,
            "type": "string",
            "required": true,
            "enum": ["task"]
        },
        "id": {
            "uniqueItems": false,
            "type": "string",
            "required": true
        },
        "comment": {
            "uniqueItems": false,
            "type": "string",
            "required": false
        },
        "nodeVariables": {
            "uniqueItems": true,
            "type": "container",
            "items":{
              "$ref":"Property"
            }
        },
        "worflowVariables": {
            "uniqueItems": true,
            "type": "container",
            "items":{
              "$ref":"Property"
            }
        }

    }

}