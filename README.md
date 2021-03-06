# Analyze GDELT data for presidential conduct

There is so much conflicting information out there about the US president that
it is hard to make sense of without being biased by ones sources. 
The only way out is to use *all* sources. Impossible?

No! The [GDELT](https://www.gdeltproject.org/) data contains summaries of a large 
part of all press communication world-wide.

These summaries include 
* date and time
* location
* involved parties
* a coding of the reported activity

I wrote this small program that filters all of this data for mentions of the US president
(luckily one clearly identifiable actor, will not work for your mom).
The activity coding of the resulting records is translated into a 
positive/negative rating according to the following scheme:

* +3 materially positive (improving situation)
* +2 explicitly positive
* +1 potentially positive (cooperative)
*  0 neutral
* -1 explitly negative
* -2 materially negative
* -3 lethal

There are options for further filtering the target of the presidential speech or action.
It should be very easy to adapt this to other important actors.
See the source for more details.

# Prerequisites

* Java 1.8+
* [Maven](https://maven.apache.org/download.cgi)
* Shell (Linux or Cygwin) for easy download of the GDELT data

# Usage

Download all GDELT data files from the 
[GDELT data site](http://data.gdeltproject.org/events/).
This can be conveniently done with the helper script 

    bash download_gdelt.sh

While the download runs you can build the application with

    mvn install

Then run

    java -jar target/gdelt-1.0-SNAPSHOT-with-dependencies.jar \
        <path-to-download-dir> <file-pattern> <output-csv-file> {filter}*

The file-pattern is a Java RegEx and should usually be ".*zip".
The filter can be a comma-separated list of
* ALL = unfiltered
* RELIG = any religious group/actor
* ETHNIC = any ethnic group/actor
* JEW = Jewish group/actor
* MOS = Muslim group/actor
* CHR = Christian group/actor

Ethnic group codes can also be given but it is recommended to just zse ETHNIC
due to the high number of ethic groups.

For more see [GEDELT CAMEO Manual](https://www.gdeltproject.org/data/documentation/CAMEO.Manual.1.1b3.pdf)

# Examples
Quick results for 2021:

    java -jar target/gdelt-1.0-SNAPSHOT-with-dependencies.jar . "2021.*zip" out2021 ALL

Full processing for multiple filter targets (may take multiple hours):

    java -jar target/gdelt-1.0-SNAPSHOT-with-dependencies.jar . ".*zip" out ALL,MOS,CHR,JEW,ETHNIC

The examples assume that the download has been run in the main directory (".").

# Output

The output file contains the number of the mentions of the US president
in the GDELT data set aggregated per month. 
For each rating level the number of presidential acts during that month is tallied.
This can be easily diagrammed with a tool of your choice.

Best results with a 100% stacked diagram excluding neutral counts. 

Here is an example with filter "ALL":

![Presidential Conduct](presidential_conduct_ALL.png)
