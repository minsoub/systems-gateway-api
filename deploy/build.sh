#!/usr/bin/env bash

eval "cd .."
eval "./gradlew clean build -x test"