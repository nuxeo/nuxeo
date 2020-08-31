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

// This script analyzes the blobs in a repository in order to extract
// information about their total size.

// ==========

var DBNAME = "nuxeo";
var COLLNAME = "default";
var DEST_COLLNAME = COLLNAME + "_blobinfo";

var QUERY = {};
var PROPS = [
  'content',
  'files/*/file',
//  'vid:transcodedVideos/*/content',
//  'thumb:thumbnail',
//  'views/*/content',
];


// ==========


db = db.getSiblingDB(DBNAME);
coll = db.getCollection(COLLNAME);
destColl = db.getCollection(DEST_COLLNAME);
print("Using     " + coll.getFullName());
print("Output to " + destColl.getFullName());

// ==========

// determine id field

var nativeId = coll.findOne({_id: '00000000-0000-0000-0000-000000000000'}) != null || coll.findOne({_id: NumberLong(0)}) != null;
var idKey = nativeId ? '_id' : 'ecm:id';

// ==========

var outputBatch = [];
function output(doc, content) {
  if (!('data' in content)) return;
  var info = {
    'id': doc[idKey],
    'name': doc['ecm:name'],
    'created': doc['dc:created'],
    // todo path (very costly)
    // todo xpath
    'key': content.data,
    'length': content.length,
  }
  if (doc['ecm:isVersion']) {
    info['version'] = true;
    info['live'] = doc['ecm:versionSeriesId'];
  }
  if (doc['ecm:isProxy']) {
    info['proxy'] = true;
    info['target'] = doc['ecm:proxyTargetId'];
  }

  outputBatch.push(info);
  if (outputBatch.length % 100) return;
  outputFlush();
}

function outputFlush() {
  if (outputBatch.length == 0) return;
  destColl.insert(outputBatch, {writeConcern: {w: 0}});
  outputBatch = [];
}


var nProcessed = 0;
var processDisplayFirst = true;
var processDisplayLastTime = Date.now();
function logProcessed() {
  nProcessed++;
  if (Date.now() - processDisplayLastTime < 5000) return; // display every 5 seconds
  processDisplayLastTime = Date.now();
  logProcessedDisplay();
}

function logProcessedDisplay() {
  if (processDisplayFirst) {
    processDisplayFirst = false;
  } else {
    print("\033[A\033[A"); // move up
  }
  print("Docs processed: " + nProcessed);
}

// ==========

print(Date() + " Starting");
print();

// list of tuples. Each element of the tuple is a basic traversal,
// after which a list iteration must be done before doing further traversals
var allprops = PROPS.map(p => p.split('/*/').map(e => e.split('/')));

print("Counting docs...");
var ndocs = coll.count(QUERY);
print("Number of docs to scan: " + ndocs);

print("Dropping destination collection...");
destColl.drop();

print("Processing documents...");

coll.find(QUERY).forEach(doc => {
  logProcessed();

  nextprop:
  for (let p of allprops) {
    var d = doc;
    for (let step of p[0]) {
      if (!(step in d)) continue nextprop;
      d = d[step];
    }
    if (p.length == 1) {
      output(doc, d);
      continue nextprop;
    }

    if (!Array.isArray(d)) continue nextprop;
    nextsubdoc:
    for (let dd of d) {
      d = dd;
      for (let step of p[1]) {
        if (!(step in d)) continue nextsubdoc;
        d = d[step];
      }
      output(doc, d);
    }
  }
});
outputFlush();
logProcessedDisplay();

print("Done processing documents");
print("Computing blobs statistics...");
print(Date());

var nBlobs = destColl.count();
print("  Number of blobs: " + nBlobs);
print(Date());

var ag = destColl.aggregate([{$group: {_id: null, sum: {$sum: '$length'}}}], {allowDiskUse: true});
var sizeBlobs = ag.next().sum;
print("  Size without deduplication: " + sizeBlobs);
print(Date());

var ag = destColl.aggregate([{$group: {_id: '$key'}}, {$group: {_id: null, count: {$sum: 1}}}], {allowDiskUse: true});
var nBlobsDedup = ag.next().count;
print("  Number of deduplicated blobs: " + nBlobsDedup);
print(Date());

var ag = destColl.aggregate([{$group: {_id: '$key', 'length': {$last: '$length'}}}, {$group: {_id: null, sum: {$sum: '$length'}}}], {allowDiskUse: true});
var sizeBlobsDedup = ag.next().sum;
print("  Size with deduplication: " + sizeBlobsDedup);

print();
print(Date() + " Done");
