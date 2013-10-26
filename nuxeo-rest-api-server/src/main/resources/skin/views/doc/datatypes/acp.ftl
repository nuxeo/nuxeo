"Ace" : {
      "uniqueItems": false,
      "properties": {
        "username": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "permission": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "granted": {
          "uniqueItems": false,
          "type": "boolean",
          "required": true
        }

      }
    },
    "Acl" : {
      "id":"Acl",
      "uniqueItems": false,
      "properties": {
        "name": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "ace": {
          "uniqueItems": false,
          "type": "array",
          "items": {
            "$ref":"Ace"
          },
          "required": true
        }
      }
    },
    "Acp" : {
      "id":"Acp",
      "uniqueItems": false,
      "properties": {
        "entity-type": {
            "uniqueItems": false,
            "type": "string",
            "required": true
        },
        "acls": {
          "uniqueItems": false,
          "type": "array",
          "items": {
            "$ref":"Acl"
          }
        }
      }
    }