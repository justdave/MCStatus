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

l10n_found = {}
language = '*'
if len(sys.argv) >= 2:
    language = sys.argv[1]
    l10n_found[language] = 1

# localizable_files: array of descriptions of files to test
#   source: a fileglob that matches a set of files that can be localized
#   localized: a fileglob that matches the same files, but localized
#   src_id_re: and
#   dst_id_re: regexp substitution patterns used to generate a unique
#       ID based on the pathname of the file. src_id_re is run on the
#       source file, and dst_re_id is run on the localized file. Both
#       should result in identical strings, i.e. the source file and its
#       corresponding localized file should have the same ID. The first
#       value is a pattern to be matched, the second is a string to replace
#       the matched part with. It can be a simple search and replace, or
#       you can you group matches () and backreferences \\1
#   lang_re: a regexp which containing a group () that matches the language
#       code in the path of the localized file
localizable_files = [
    {
        'source':    'playstore/en/*.txt',
        'localized': 'playstore/{}/*.txt'.format(language),
        'src_id_re': ['^playstore/en/', 'playstore/'],
        'dst_id_re': ['^playstore/[^/]+/', 'playstore/'],
        'lang_re':   '^playstore/([^/]+)/'
        },
    {
        'source':    'app/src/main/res/values/strings.xml',
        'localized': 'app/src/main/res/values-{}/strings.xml'.format(language),
        'src_id_re': ['no-op', 'no-op'], # the original path is fine
        'dst_id_re': ['^app/src/main/res/values-[^/]+/', 'app/src/main/res/values/'],
        'lang_re':   '^app/src/main/res/values-([^/]+)/'
        },
    {
        'source':    'app/src/main/res/raw/*.txt',
        'localized': 'app/src/main/res/raw-{}/*.txt'.format(language),
        'src_id_re': ['no-op', 'no-op'], # the original path is fine
        'dst_id_re': ['^app/src/main/res/raw-[^/]+/', 'app/src/main/res/raw/'],
        'lang_re':   '^app/src/main/res/raw-([^/]+)/'
        },
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
    print(run_with_output('env'))
    print("{}======================{}".format(GREEN,ENDC))

branch = run_with_output('git branch --show-current').strip('\n')
errors = 0
for fileglob in localizable_files:
    output = run_with_output('git ls-files {} | xargs -n1 git log --format=format:"~ %aI" --name-only -n1 {} --'.format(fileglob['source'],branch))
    currentdate = ""
    for line in output.splitlines():
        if DEBUG:
            print(line)
        if line[:1] == "~":
            currentdate = datetime.fromisoformat(line[2:])
        else:
            if DEBUG:
                print('search: {}'.format(fileglob['src_id_re'][0]))
                print('search: {}'.format(fileglob['src_id_re'][1]))
            fileid = re.sub(fileglob['src_id_re'][0],fileglob['src_id_re'][1],line)
            default_files[fileid] = {
                'filename': line,
                'commit_date': currentdate,
                'l10n_found': {}
                }
            if DEBUG:
                print('fileid: {}'.format(fileid))
                print(str(default_files[fileid]))
    output = run_with_output('git ls-files {} | grep -v "playstore/en/" | xargs -n1 git log --format=format:"~ %aI" --name-only -n1 {} --'.format(fileglob['localized'],branch))
    currentdate = ""
    for line in output.splitlines():
        if DEBUG:
            print(line)
        if line[:1] == "~":
            currentdate = datetime.fromisoformat(line[2:])
        else:
            fileid = line
            lang = '*'
            m = re.match(fileglob['lang_re'], line)
            if m:
                lang = m.group(1)
            fileid = re.sub(fileglob['dst_id_re'][0], fileglob['dst_id_re'][1], line)
            default_files[fileid]['l10n_found'][lang] = 1;
            l10n_found[lang] = 1;
            if (currentdate.timestamp() >= default_files[fileid]['commit_date'].timestamp()):
                if DEBUG:
                    print("v- localized: {} newer than source: {}".format(currentdate,default_files[fileid]['commit_date']))
                print("[{}PASS{}] {} is up-to-date.".format(GREEN,ENDC,line))
            else:
                if DEBUG:
                    print("v- localized: {} older than source: {}".format(currentdate,default_files[fileid]['commit_date']))
                errors = errors + 1
                print("[{}FAIL{}] {} is outdated.".format(RED,ENDC,line))

if DEBUG:
    print('l10n_found: {}'.format(list(l10n_found.keys())))
for filename in default_files:
    for lang in l10n_found:
        if not lang in default_files[filename]['l10n_found']:
            print('[{}FAIL{}] {} is not localized for {}.'.format(RED,ENDC,default_files[filename]['filename'],lang))
            errors = errors + 1


if (errors > 0):
    print("{}{} outdated localization files found.{}".format(RED,errors,ENDC))
    sys.exit(-1)
else:
    print("All localization files up-to-date.")

