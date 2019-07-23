enscript -R --file-align=2 --color --highlight --line-numbers -o - `find . -name '*.java'` | ps2pdf - epq_source.pdf
