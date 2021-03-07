#!/usr/bin/env python3

import sys
import subprocess
from subprocess import PIPE,STDOUT
import re
from datetime import datetime

DEBUG = 1
GREEN = '\033[92m'
RED = '\033[91m'
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

def run_with_output(string):
    if DEBUG:
        print("{}{}{}".format(GREEN,string,ENDC))
    return subprocess.run(string, shell=True,stdout=PIPE,stderr=STDOUT).stdout.decode('utf-8')

default_files = {}
if DEBUG:
    print("{}===== DEBUG INFO ====={}".format(GREEN,ENDC))
    print(run_with_output("git version"))
    print(run_with_output('git branch --show-current'))
    print(run_with_output('git config -l'))
    print("{}The following should show 'More hard-coded strings localizable' on Mar 4:{}".format(GREEN,ENDC))
    print(run_with_output('git log -n1 master -- app/src/main/res/values/strings.xml'))
    print(run_with_output('env'))
    print("{}======================{}".format(GREEN,ENDC))

branch = run_with_output('git branch --show-current').strip('\n')
for fileglob in localizable_files:
    output = run_with_output('git ls-files {} | xargs -n1 git log --format=format:"~ %aI" --name-only -n1 {} --'.format(fileglob,branch))
    currentdate = ""
    for line in output.splitlines():
        if DEBUG:
            print(line)
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
    output = run_with_output('git ls-files {} | grep -v "playstore/en/" | xargs -n1 git log --format=format:"~ %aI" --name-only -n1 {} --'.format(fileglob,branch))
    currentdate = ""
    for line in output.splitlines():
        if DEBUG:
            print(line)
        if line[:1] == "~":
            currentdate = datetime.fromisoformat(line[2:])
        else:
            fileid = re.sub('(playstore/)[^/]+/', '\\1', line)
            fileid = re.sub('(main/res/[^-/]+)-[^/]+', '\\1', fileid)
            if (currentdate.timestamp() >= default_files[fileid]['commit_date'].timestamp()):
                if DEBUG:
                    print("v- localized: {} newer than source: {}".format(currentdate,default_files[fileid]['commit_date']))
                print("[{}PASS{}] {} is up-to-date.".format(GREEN,ENDC,line))
            else:
                if DEBUG:
                    print("v- localized: {} older than source: {}".format(currentdate,default_files[fileid]['commit_date']))
                errors = errors + 1
                print("[{}FAIL{}] {} is outdated.".format(RED,ENDC,line))

if (errors > 0):
    print("{}{} outdated localization files found.{}".format(RED,errors,ENDC))
    sys.exit(-1)
else:
    print("All localization files up-to-date.")

