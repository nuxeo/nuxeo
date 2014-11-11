#!/usr/bin/python

import logging
import os
import sys
import shutil

logging.basicConfig(level=logging.DEBUG)

try:
    import pip
except ImportError:
    print 'ERROR: Module pip is required!'
    sys.exit(1)

url = 'git+https://github.com/apache/chemistry-cmislib.git@trunk#egg=cmislib'

buildDir = './target/python_build'
if os.path.exists(buildDir):
    shutil.rmtree(buildDir)

pipcode = pip.main(['install', url, '-t', buildDir])
if pipcode != 0:
    print 'ERROR: Unable to install module from url: ' + url

installDir = '.'

root_src_dir = buildDir + '/'
root_dst_dir = installDir + '/'

for src_dir, dirs, files in os.walk(root_src_dir):
    logging.debug("src_dir: " + src_dir)
    if src_dir == root_src_dir or src_dir.endswith('.egg-info'):
        continue
    dst_dir = src_dir.replace(root_src_dir, root_dst_dir)
    logging.debug("dst_dir: " + dst_dir)
    if os.path.exists(dst_dir):
        shutil.rmtree(dst_dir)
    shutil.copytree(src_dir, dst_dir)
