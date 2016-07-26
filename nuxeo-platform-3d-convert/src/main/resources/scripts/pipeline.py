import bpy, json, os, sys
from copy import copy
from math import pi, cos, sin, degrees
from mathutils import Vector
from blendergltf import blendergltf
import argparse

parser = argparse.ArgumentParser(description='Blender pipeline.')
parser.add_argument('-operators',  nargs='*', choices=['import','lod','render','convert'],
                    help='a list of operators to run in the pipeline (options: lod,render,export)')
parser.add_argument('-lods',  nargs='*',
                    help='a list od level of details to use ont these operator (options: 0-100)')
parser.add_argument('-input', help='path for the input file')
parser.add_argument('-outdir', help='path for output dir')

parser.add_argument('-width', help='render width')
parser.add_argument('-height', help='render height')

args_to_parse = sys.argv[sys.argv.index('--')+1:]
args = parser.parse_args(args_to_parse)
print("opeartors: ")
print(args)
if args.operators == None:
    sys.exit()

base_path = os.path.dirname(os.path.abspath(__file__)) + "/pipeline/"
lod=100
for operator in args.operators:
    print("Running: " + operator)
    if operator == "lod" and args.lods and len(args.lods):
        lod = int(args.lods.pop())
    filename = base_path + operator + ".py"
    exec(compile(open(filename).read(), filename, 'exec'))
