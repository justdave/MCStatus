#!/usr/bin/env python3

import sys
import subprocess
import re
from datetime import datetime

class bcolors:
    OKGREEN = '\033[92m'
    WARNING = '\033[91m'
    ENDC = '\033[0m'

localizable_files = [
    'playstore/en/*.txt',
    'app/src/main/res/values/strings.xml',
    'app/src/main/res/raw/*.txt'
    ]
localized_locations = [
    'playstore/*/*.txt',
    'app/src/main/res/values-*/strings.xml',
    'app/src/main/res/raw-*/*.txt'
    ]

default_files = {}
for fileglob in localizable_files:
    output = subprocess.run('git ls-files {} | xargs -n1 git log --format=format:"~ %aI" --name-only -n1'.format(fileglob), shell=True,stdout=subprocess.PIPE).stdout.decode('utf-8')

    currentdate = ""
    for line in output.splitlines():
        if line[:1] == "~":
            currentdate = datetime.fromisoformat(line[2:])
        else:
            fileid = line.replace('/en/','/')
            default_files[fileid] = {
                'filename': line,
                'commit_date': currentdate
                }

errors = 0
for fileglob in localized_locations:
    output = subprocess.run('git ls-files {} | grep -v "playstore/en/" | xargs -n1 git log --format=format:"~ %aI" --name-only -n1'.format(fileglob), shell=True,stdout=subprocess.PIPE).stdout.decode('utf-8')
    currentdate = ""
    for line in output.splitlines():
        if line[:1] == "~":
            currentdate = datetime.fromisoformat(line[2:])
        else:
            fileid = re.sub('(playstore/)[^/]+/', '\\1', line)
            fileid = re.sub('(main/res/[^-/]+)-[^/]+', '\\1', fileid)
            if (currentdate.timestamp() >= default_files[fileid]['commit_date'].timestamp()):
                print("[{}PASS{}] {} is up-to-date. localized: {} newer than source: {}".format(bcolors.OKGREEN,bcolors.ENDC,line,currentdate,default_files[fileid]['commit_date']))
            else:
                errors = errors + 1
                print("[{}FAIL{}] {} is outdated. localized: {} older than source: {}".format(bcolors.WARNING,bcolors.ENDC,line,currentdate,default_files[fileid]['commit_date']))

if (errors > 0):
    print("{}{} outdated localization files found.{}".format(bcolors.WARNING,errors,bcolors.ENDC))
    sys.exit(-1)
else:
    print("All localization files up-to-date.")

