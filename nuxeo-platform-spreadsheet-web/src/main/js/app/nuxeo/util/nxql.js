/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt>
 */

/**
 * NXQL Query
 */
class NXQLQuery {
  constructor(nxql) {
    var result = NXQLRE.exec(nxql);
    if (!result) {
      throw `Failed to parse NXQL: ${nxql}`;
    }
    this.parts = result.slice(1);
  }

  get selectClause() { return this.parts[0]; }
  set selectClause(c) { this.parts[0] = c; }

  get fromClause() { return this.parts[1]; }
  set fromClause(c) { this.parts[1] = c; }

  get whereClause() { return this.parts[2]; }
  set whereClause(c) { this.parts[2] = c; }

  get orderClause() { return this.parts[3]; }
  set orderClause(c) { this.parts[3] = c; }

  /**
   * Add a where clause
   * @param clause
   */
  set addWhereClause(clause) {
    if (this.whereClause) {
      this.whereClause += ` AND ${clause}`;
    } else {
      this.whereClause = `WHERE ${clause}`;
    }
  }

  toString() {
    return this.parts.join(' ');
  }
}

/**
 * RegExp to parse a NXQL query
 */
var NXQLRE = new RegExp(
        '(SELECT\\s.*)' +
        '(FROM\\s(?:.(?!WHERE|ORDER BY))*)\\s?' +
        '(WHERE\\s.(?:.(?!ORDER BY))*)?\\s?' +
        '(ORDER BY\\s.*)?',
    'i');

/**
 * Fixed conditions to add to a NXQL query
 */
const QUERY_FIXED_PART =
    'ecm:mixinType != \'HiddenInNavigation\' AND ' +
    'ecm:isProxy = 0 AND ' +
    'ecm:isVersion = 0 AND ' +
    'ecm:currentLifeCycleState != \'deleted\'';

/**
 * Parses a NXQL query and add the fixed part
 */
export function parseNXQL(nxql) {
  var result = nxql;
  try {
    var query = new NXQLQuery(nxql);
    query.addWhereClause = QUERY_FIXED_PART;
    result = query.toString();
  } catch (err) {
    console.log(err);
  }
  return result;
}