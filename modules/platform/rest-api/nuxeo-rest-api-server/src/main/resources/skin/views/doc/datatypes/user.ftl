"GroupRef" : {
  "id": "GroupRef",
  "uniqueItems": false,
  "properties": {
    "name": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "label": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "url": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    }
   }
 },

"user" : {
  "id": "user",
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
    "properties": {
      "uniqueItems": false,
      "type": "container",
      "items":{
          "$ref":"Property"
        }
    },
    "extendedGroups": {
      "uniqueItems": false,
      "type": "container",
      "items":{
          "$ref":"GroupRef"
        }
    },
    "isAdministrator": {
      "uniqueItems": false,
      "type": "boolean",
      "required": true
    },
    "isAnonymous": {
      "uniqueItems": false,
      "type": "boolean",
      "required": true
    }
  }
},
"userList" : {
  "id": "userList",
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
        "$ref":"user"
      },
      "required": true
    }

  }
}
