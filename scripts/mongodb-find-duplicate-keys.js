// (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// Contributors:
//     Florent Guillaume
// v1.1

// This script attempts to remove identical documents with duplicate ecm:id.

var DBNAME = "nuxeo"
var COLLNAME = "default"
var DRY_RUN = true
var SHOW_UNRESOLVED = true

db = db.getSiblingDB(DBNAME)
coll = db.getCollection(COLLNAME)
print()
if (DRY_RUN) {
  print("DRY RUN no modifications will be done")
}
print("Using " + DBNAME + "." + COLLNAME)

print("Collection has " + coll.estimatedDocumentCount() + " documents")

// function to compare documents - this is specific to MongoDB + Nuxeo
// derived from https://github.com/epoberezkin/fast-deep-equal
function nuxeoEquals(a, b) {
  if (a === b) return true
  if (a && b && typeof a == 'object' && typeof b == 'object') {
    if (a.constructor !== b.constructor) return false
    // compare arrays
    if (Array.isArray(a)) {
      if (a.length != b.length) return false
      for (var i = a.length; i-- !== 0;) {
        if (!nuxeoEquals(a[i], b[i])) return false
      }
      return true
    }
    // check using specialized valueOf/toString
    if (a.valueOf !== Object.prototype.valueOf) return a.valueOf() === b.valueOf()
    if (a.toString !== Object.prototype.toString)  return a.toString() === b.toString()
    // compare all object keys
    var keys = Object.keys(a)
    var length = keys.length
    if (length !== Object.keys(b).length) return false
    for (var i = length; i-- !== 0;) {
      if (!Object.prototype.hasOwnProperty.call(b, keys[i])) return false
    }
    for (var i = length; i-- !== 0;) {
      var key = keys[i]
      if (key === '_id') continue
      if (!nuxeoEquals(a[key], b[key])) return false
    }
    return true
  }
  // true if both NaN, false otherwise
  return a!==a && b!==b
}

// check duplicates and remove identical ones
var nbDupes = 0
var nbDocumentsImpacted = 0
var nbDocumentsRemoved = 0
var nbResolvedDupes = 0
var firstUnresolvedShown = false
print("Starting scan for duplicate ids...")
var agg = coll.aggregate([
  {"$group": {"_id": "$ecm:id", "count": {"$sum": 1}, objectids: {$addToSet: "$_id"}}},
  {"$match": {"count": {"$gt": 1}}},
  {"$project": {"ecm:id": "$_id", "objectids": "$objectids", "_id": 0}}],
  {allowDiskUse: true})
agg.forEach(function(group) {
    nbDupes++
    var docs = coll.find({"_id": {"$in": group.objectids}}).toArray()
    var len = docs.length
    nbDocumentsImpacted += len
    var n = 0 // removed with this ecm:id
    for (var i = 0; i < len-1; i++) {
      var reference = docs[i]
      if (!reference) continue // already removed
      for (var j = i+1; j < len; j++) {
        var doc = docs[j]
        if (!doc) continue // already removed
        if (nuxeoEquals(reference, doc)) {
          // doc is identical to reference, remove it
          if (!DRY_RUN) {
            coll.remove({"_id": doc["_id"]})
          }
          docs[j] = null
          n++
          nbDocumentsRemoved++
        }
      }
    }
    if (n == len - 1) {
      nbResolvedDupes++
    } else if (SHOW_UNRESOLVED) {
      if (!firstUnresolvedShown) {
        print("Showing unresolved duplicate ids")
        firstUnresolvedShown = true
      }
      print()
      print("ecm:id = " + group["ecm:id"])
      for (var i = 0; i < len; i++) {
        var doc = docs[i]
        if (!doc) continue
        printjson(doc)
      }
    }
  })

var nbUnresolvedDupes = nbDupes - nbResolvedDupes
print("Collection has " + nbDupes + " duplicate ids")
print("Collection has " + nbDocumentsImpacted + " documents impacted by duplicates")
print("Collection has " + nbDocumentsRemoved + " identical documents that were removed")
print("Collection has " + nbResolvedDupes + " resolved duplicate ids")
print("Collection has " + nbUnresolvedDupes + " unresolved duplicate ids")

quit(nbUnresolvedDupes ? 1 : 0)
