"OperationParams" : {
      "id":"OperationParams",
      "uniqueItems": false,
      "properties": {
        "params": {
          "uniqueItems": false,
          "type": "object",
          "required": true

        },
        "context": {
          "uniqueItems": false,
          "type": "object",
          "required": true
        }

      }
    },
    "OperationParamDescription" : {
      "id":"OperationParamDescription",
      "uniqueItems": false,
      "properties": {
        "name": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "description": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "type": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "required": {
      "uniqueItems": false,
      "type": "boolean",
      "required": true
    },
    "widget": {
      "uniqueItems": false,
      "type": "string",
      "required": false
    },
    "order": {
      "uniqueItems": false,
      "type": "long",
      "required": false
    },
    "values": {
      "uniqueItems": false,
      "type": "array",
      "items":{
        "type":"string"
      },
      "required": false
    }
  }
},

"OperationDescription" : {
      "id":"OperationDescription",
      "uniqueItems": false,
      "properties": {
        "id": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "label": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "category": {
          "uniqueItems": false,
          "type": "string",
          "required": false
        },
        "requires": {
          "uniqueItems": false,
          "type": "string",
          "required": false
        },
        "description": {
          "uniqueItems": false,
          "type": "string",
          "required": false
        },
        "url": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "signature": {
          "uniqueItems": false,
          "type": "array",
          "items": {
            "type":"string"
          },
          "required": true
        },
        "params": {
          "uniqueItems": false,
          "type": "array",
          "items": {
            "$ref":"OperationParamDescription"
          },
          "required": true
        }
      }
    },

    "OperationDescriptionList" : {
  "id":"OperationParamDescriptionList",
  "uniqueItems": false,
    "properties": {
      "path": {
          "uniqueItems": false,
          "type": "container",
          "required": true
        },
      "codec": {
          "uniqueItems": false,
          "type": "array",
          "items": {
            "type":"string"
          },
          "required": true
        },
      "operations": {
          "uniqueItems": false,
          "type": "array",
          "items": {
            "$ref":"OperationDescription"
          },
          "required": true
        }
    }
 }
