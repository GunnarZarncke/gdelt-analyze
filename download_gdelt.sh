
function gget() {

  if [ -f "$1" ]; then
    echo "$1 exists"
  else
    echo -n "get $1 "
    wget -q http://data.gdeltproject.org/events/$1
    if [ $? == 0 ]; then echo "DONE"; else echo "FAILED"; fi
  fi
}

for y in {1979..2005}; do
  gget $y.zip
done

for y in {2006..2013}; do
  for m in 12 11 10 09 08 07 06 05 04 03 02 01; do
     gget $y$m.zip
  done
done
for y in {2013..2022}; do
  for m in 12 11 10 09 08 07 06 05 04 03 02 01; do
    for d in 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31; do
      gget $y$m$d.export.CSV.zip
    done
  done
done
