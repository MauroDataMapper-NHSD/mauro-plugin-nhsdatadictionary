#!/usr/bin/env bash
set -euo pipefail

pattern_dir=${1:?need pattern dir}
script_file=${2:-script.sed}

escape_line() {
  # Escape regex metacharacters and the delimiter used in the generated sed command.
  # This is enough for your HTML fragments and is much safer than Bash glob replacement.
  printf '%s' "$1" | sed 's/[][\\.^$*+?(){}|/&]/\\&/g'
}

escape_pattern_file() {
  local file=$1
  local line first=1

  while IFS= read -r line || [[ -n "$line" ]]; do
    line=$(escape_line "$line")
    if [[ $first -eq 0 ]]; then
      printf '\\n'
    fi
    printf '%s' "$line"
    first=0
  done < "$file"
}

{
  printf '%s\n' ':a'
  printf '%s\n' 'N'
  printf '%s\n' '$!ba'

  for pf in "$pattern_dir"/*; do
    [[ -f "$pf" ]] || continue
    printf 's|'
    escape_pattern_file "$pf"
    printf '||g\n'
  done
} > "$script_file"