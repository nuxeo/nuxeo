"workflow" : {
    "id": "workflow",
    "type": "any",
    "required": false,
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
            "required": false
        },
        "name": {
            "uniqueItems": false,
            "type": "string",
            "required": false
        },
        "initiator": {
            "uniqueItems": false,
            "type": "string",
            "required": false
        },
        "attachedDocumentIds": {

            "type": "array",
            "items" : {
              "type":"string"
            },
            "required": false
        },
		"variables": {
            "uniqueItems": true,
            "type": "container",
            "items":{
              "$ref":"Property"
            }
        }

    }

},

 "workflows" : {
      "id": "workflows",
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
            "$ref":"workflow"
          },
          "required": true
        }

      }
},
