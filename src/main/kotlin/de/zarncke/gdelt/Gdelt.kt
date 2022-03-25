package de.zarncke.gdelt

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import java.io.*
import java.lang.Exception
import java.time.OffsetDateTime
import java.util.zip.ZipInputStream

// map GDELT event codes onto a simpler classification
//
// +3 materially positive (actual improvements)
// +2 explicitly positive (in word or deed)
// +1 potentially positive (cooperative)
//  0 neutral
// -1 explicitly negative (in word or deed)
// -2 materially negative (negative consequences)
// -3 lethal (actual or suspected death)
// -4 war, mass death
//
// See http://data.gdeltproject.org/documentation/CAMEO.Manual.1.1b3.pdf
val scores = mapOf<String, Int>(
    "010" to 0,
    "011" to 0,
    "012" to -1,
    "013" to 2,
    "014" to 0,
    "015" to 0,
    "016" to -1,
    "017" to 1,
    "018" to 1,
    "019" to 2,

    "020" to 0,
    "021" to 1, // strictly depends on sub code
    "022" to 1,
    "023" to 2, // strictly depends on sub code
    "024" to 1,
    "025" to 1,
    "026" to 0,
    "027" to 1,
    "028" to 1,

    "030" to 1,
    "031" to 1,
    "032" to 1,
    "033" to 2,
    "034" to 0,
    "035" to 2,
    "036" to 0,
    "037" to 1,
    "038" to 1,
    "039" to 1,

    "040" to 1,
    "041" to 0,
    "042" to 0,
    "043" to 0,
    "044" to 0,
    "045" to 1,
    "046" to 0,

    "050" to 1,
    "051" to 2,
    "052" to 2,
    "053" to 1,
    "054" to 2,
    "055" to 2,
    "056" to 2,
    "057" to 0,

    "060" to 2,
    "061" to 3,
    "062" to 2,
    "063" to 2,
    "064" to 2,

    "070" to 3,
    "071" to 3,
    "072" to 2,
    "073" to 3,
    "074" to 3,
    "075" to 3,

    "080" to 2,
    "081" to 3,
    "082" to 2,
    "083" to 3,
    "084" to 3,
    "085" to 3,
    "086" to 2,
    "087" to 3,

    "090" to 1,
    "091" to 1,
    "092" to 1,
    "093" to 1,
    "094" to 1,

    "100" to -1,
    "101" to -1,
    "102" to 0,
    "103" to 0,
    "104" to -1,
    "105" to 0,
    "106" to 0,
    "107" to 0,
    "108" to 1,

    "110" to -1,
    "111" to -1,
    "112" to -1,
    "113" to -1,
    "114" to 0,
    "115" to -1,
    "116" to -1,

    "120" to -1,
    "121" to -1,
    "122" to -1,
    "123" to -1,
    "124" to -2,
    "125" to -1,
    "126" to -1,
    "127" to -1,
    "128" to -2,
    "129" to 0,

    "130" to 1,
    "131" to -1,
    "132" to -1,
    "133" to -1,
    "134" to -1,
    "135" to -1,
    "136" to -1,
    "137" to -1,
    "138" to -2, // extreme here
    "139" to -1,

    "140" to 0,
    "141" to 0,
    "142" to 0,
    "143" to -1,
    "144" to -1,
    "145" to -2,

    "150" to -2,
    "151" to -1,
    "152" to -2,
    "153" to -1,
    "154" to -2,
    "155" to -1,

    "160" to -1,
    "161" to -1,
    "162" to -1,
    "163" to -2,
    "164" to -1,
    "165" to -1,
    "166" to -1,

    "170" to -2,
    "171" to -2,
    "172" to -2,
    "173" to -2,
    "174" to -2,
    "175" to -2,
    "176" to -2,

    "180" to -2,
    "181" to -2,
    "182" to -3,
    "183" to -3,
    "184" to -2,
    "185" to -2,
    "186" to -3,

    "190" to -3,
    "191" to -2,
    "192" to -2,
    "193" to -3,
    "194" to -4,
    "195" to -3,
    "196" to -3,

    "200" to -4,
    "201" to -3,
    "202" to -4,
    "203" to -4,
    "204" to -4
)

private fun dump(outFile: String, map: MutableMap<Int, MutableMap<Int, Int>>) {
    OutputStreamWriter(FileOutputStream(File(outFile)), "UTF-8").use { out ->
        out.write("month;3;2;1;0;-1;-2;-3;-4")
        out.write(System.lineSeparator())
        for (month in map.keys.sorted()) {
            val mes = map[month]!!
            val line = "$month;${mes[3] ?: 0};${mes[2] ?: 0};${mes[1] ?: 0};${mes[0] ?: 0};${
            mes[-1] ?: 0
            };${mes[-2] ?: 0};${mes[-3] ?: 0};${mes[-4] ?: 0}"
            out.write(line)
            out.write(System.lineSeparator())
            out.flush()
        }
    }
}

fun procZip(zip: File, map: MutableMap<Int, MutableMap<Int, Int>>, target: String) {
    ZipInputStream(BufferedInputStream(FileInputStream(zip), 16384)).use {
        while (true) {
            val entry = it.nextEntry ?: break
            val csv = CSVReaderBuilder(
                InputStreamReader(it, "ASCII")
            ).withCSVParser(CSVParserBuilder().withSeparator('\t').build()).build()
            var l = 1
            while (true) {
                val next = csv.readNext() ?: break
                process(next, l, map, target)
                l++
            }
        }
    }
}

private fun process(
    record: Array<out String>,
    lineNo: Int,
    map: MutableMap<Int, MutableMap<Int, Int>>,
    target: String
) {
    // for data format see http://data.gdeltproject.org/documentation/GDELT-Event_Codebook-V2.0.pdf

    if (record.size < 27) return
    val date = try {
        record[2].toInt() // months number
    } catch (e: Exception) {
        System.err.println("invalid month number ${record[2]} (ignored)")
        return
    }
    val actor = record[6]
    val cameo = record[26]
    val score = scores[cameo] ?: 0
    val state = record[37]
    val targetActorEth = record[19] // ethnic
    val targetActorRel = record[20] // religion

    if (state == "US" && actor == "PRESIDENT") { // filter for US president
        // filter for targets of the president's action
        if (target == "ALL" ||
            target == "RELIG" && targetActorRel.isNotBlank() ||
            target == "ETHNIC" && targetActorEth.isNotBlank() ||
            target == targetActorRel ||
            target == targetActorEth
        ) {
            val forDate = map.getOrPut(date, { mutableMapOf() })
            forDate[score] = (forDate[score] ?: 0) + 1
        }
    }
}

/**
 * Filter values:
 * * ALL = unfiltered
 * * RELIG = any religious group/actor
 * * ETHNIC = any ethnic group/actor
 * Religion of ethnic group codes can also be given. Example religions are:
 * * JEW = Judaism
 * * MOS = Islam
 * * CHR = Christianity
 * for more see https://www.gdeltproject.org/data/documentation/CAMEO.Manual.1.1b3.pdf
 */
fun main(args: Array<String>) {
    if (args.size < 3) {
        System.err.println("At least source dir, pattern, and output-prefix must be given. See README.md.")
        System.exit(1)
    }
    val inDir = args[0]
    val regex = args[1].toRegex()
    val outFilePrefix = args[2]
    val filter = if (args.size == 3) listOf("ALL") else args.drop(3).flatMap { it.split(",") }

    System.err.println("Parsing $filter in $inDir/$regex")

    for (target in filter) {

        val map = mutableMapOf<Int, MutableMap<Int, Int>>()

        val outFile = "$outFilePrefix-$target.csv"
        File(inDir).listFiles { _, name -> name.matches(regex) }
            .sortedBy { it.name }
            .forEach { inFile ->
                try {
                    System.err.println("parsing $target in $inFile (time: ${OffsetDateTime.now()})")
                    procZip(inFile, map, target)
                    dump(outFile, map) // continuously updated
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        System.err.println("Result for $target ib $outFile")
    }
}
