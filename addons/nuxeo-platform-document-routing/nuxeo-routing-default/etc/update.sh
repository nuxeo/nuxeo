#!/bin/sh
# (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the GNU Lesser General Public License
# (LGPL) version 2.1 which accompanies this distribution, and is available at
# http://www.gnu.org/licenses/lgpl-2.1.html
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# Contributors:
#     Florent Guillaume
#     Guillaume Renard

if [ -z "$1" ]; then
  echo Usage: update.sh ../path/to/the-studio-jar/unpacked >&2
  exit 2
fi
JARDIR=$1
STUDIONAME=nuxeo-routing-default
BUNDLE=$(cd `dirname $0`; pwd)/..

RES=$BUNDLE/src/main/resources

cp -R $JARDIR/data $RES/
cp -R $JARDIR/OSGI-INF/extensions.xml $RES/OSGI-INF/
# XXX do not sync the deployment fragment to impact messages.properties too, see NXS-2600

# replace studio name
find $RES -name extensions.xml -o -name '*.xsd' | while read f; do
  sed -e "s,/layouts/${STUDIONAME}_layout_template.xhtml,/layouts/layout_default_template.xhtml," \
      -e "s,$STUDIONAME,nuxeo-routing-default,g" \
      -i '~' $f
done

# replace workflow filter id to add back old id, see NXP-11633
sed -e 's,filter@wf@SerialDocumentReview,filter@SerialDocumentReview,' \
    -i '~' $RES/OSGI-INF/extensions.xml

# remove require on runtime started
sed -e 's#  <require>org.nuxeo.runtime.started</require>##' \
    -i '~' $RES/OSGI-INF/extensions.xml

# remove studio widget types extensions
sed -e '/<extension target="org.nuxeo.ecm.platform.forms.layout.WebLayoutManager" point="widgettypes"/,/<.extension>/d' \
    -i '~' $RES/OSGI-INF/extensions.xml

#temporary fix, waiting for NXS-1827 to be done    
sed -e 's/<property name=\"width\">300<\/property>/<property name=\"width\">70%<\/property>/g' \
    -i '~' $RES/OSGI-INF/extensions.xml

#temporary fix for MySQL column length, see NXP-17334
sed -e 's|  <xs:element name="review_result" type="xs:string"/>|\
  <xs:simpleType name="bigString">\
    <xs:restriction base="xs:string">\
      <xs:maxLength value="10000"/>\
    </xs:restriction>\
  </xs:simpleType>\
  <xs:element name="review_result" type="nxs:bigString"/>|' \
    -i '~' $RES/data/schemas/var_ParallelDocumentReview.xsd

# in zip, replace studio name in schema namespaces, as well as workflow filter id
# also keep them unzipped to track changes in git diffs easily
for wf in SerialDocumentReview ParallelDocumentReview; do
  ZIP=$RES/data/$wf.zip
  DIR=$RES/data/$wf
  rm -rf $DIR/
  mkdir $DIR
  cd $DIR
  unzip $ZIP
  find . -name '*.xml' | while read f; do
    sed -e "s,$STUDIONAME,nuxeo-routing-default,g" \
        -i '~' $f
    # only the serial workflow needs an old id (NXP-11633), parallel came later
    sed -e 's,filter@wf@SerialDocumentReview,filter@SerialDocumentReview,' \
        -i '~' $f
  done
  find . -name '*~' | xargs rm
  rm $ZIP
  zip -r $ZIP .
done

# Create JSF extensions.xml
BUNDLE_JSF=$BUNDLE/../$STUDIONAME"-jsf"
EXT_JSF=$BUNDLE_JSF/src/main/resources/OSGI-INF/extensions.xml
echo 'Extracting JSF contrib to' $BUNDLE_JSF
echo '<?xml version="1.0" encoding="UTF-8"?>' > $EXT_JSF
echo '<component name="studio.extensions.nuxeo-routing-default.jsf" version="1.0.0">' >> $EXT_JSF
# extract JSF contrib
DIR_UI_C=$(xmllint --xpath "//extension[@target='org.nuxeo.ecm.directory.ui.DirectoryUIManager']" $RES/OSGI-INF/extensions.xml)
echo "  $DIR_UI_C">> $EXT_JSF
LAYOUT_C=$(xmllint --xpath "//extension[@target='org.nuxeo.ecm.platform.forms.layout.WebLayoutManager']" $RES/OSGI-INF/extensions.xml)
echo "  $LAYOUT_C">> $EXT_JSF
echo '</component>' >> $EXT_JSF
# Remove JSF contribs from core extensions.xml
sed -e '/<extension target="org.nuxeo.ecm.directory.ui.DirectoryUIManager" point="directories"/,/<.extension>/d' \
    -i '~' $RES/OSGI-INF/extensions.xml
sed -e '/<extension target="org.nuxeo.ecm.platform.forms.layout.WebLayoutManager" point="layouts"/,/<.extension>/d' \
    -i '~' $RES/OSGI-INF/extensions.xml

cd $BUNDLE
