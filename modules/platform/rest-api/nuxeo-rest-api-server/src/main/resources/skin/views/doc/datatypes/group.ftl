"group" : {
  "id": "group",
  "uniqueItems": false,
  "properties": {
    "entity-type": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "groupname": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "grouplabel": {
      "uniqueItems": false,
      "type": "string",
      "required":"false"
    },
    "memberUsers": {
        "type": "array",
        "items" : {
          "type":"string"
        },
        "required": false
    },
    "memberGroups": {
        "type": "array",
        "items" : {
          "type":"string"
        },
        "required": false
    },
  }
},
"groupList" : {
  "id": "groupList",
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
        "$ref":"group"
      },
      "required": true
    }

  }
}
