#!/bin/sh
set -e

name="$1"
[ -z "$name" ] && { >&2 echo "Please specify a name"; exit 1; }
first_char="$(printf %.1s "$name")"
[ "$(echo "$first_char"  | tr "[:lower:]" "[:upper:]")" != "$first_char" ] && { >&2 echo "Name must be PascalCase"; exit 1; }

d="$name/src/main/kotlin/com.github.diamondminer88.plugins"
new="$d/$name.kt"

set -x
cp -r Template "$name"

# Renaming file
mv "$d/Template.kt" "$new"
# Change class name
sed -i "s/Template/$name/" "$new"

# Add to settings.gradle
echo "include(\":$name\")" | cat - settings.gradle.kts > settings.gradle.new && mv settings.gradle.new settings.gradle.kts