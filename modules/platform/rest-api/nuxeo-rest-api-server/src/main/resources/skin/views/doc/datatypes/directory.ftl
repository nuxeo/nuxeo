"directoryEntry" : {
  "id": "directoryEntry",
  "uniqueItems": false,
  "properties": {
    "entity-type": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "directoryName": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "properties": {
      "uniqueItems": false,
      "type": "container",
      "items":{
          "$ref":"Property"
        }
    }

  }
},
"directoryEntries" : {
  "id": "directoryEntries",
  "uniqueItems": false,
  "properties": {
    "entity-type": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    <#include "views/doc/datatypes/paginable.ftl"/>,
    "entries": {
      "uniqueItems": false,
      "type": "array",
      "items": {
        "$ref":"directoryEntry"
      },
      "required": true
    }

  }
}
