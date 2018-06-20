#!/bin/bash
set -e

mill all {aggregate[2.11.11].test,aggregate[2.12.6].test}

# Fail with java.io.FileNotFoundException: aggregate/out/aggregateJs/2.12.4/test/test/dest/out.json
# mill aggregateJs[2.11.11].test
# mill aggregateJs[2.12.4].test
