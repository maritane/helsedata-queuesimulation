package com.example.demo

import com.example.demo.QueueAlgoritm.*
import java.time.Duration
import java.time.LocalTime
import java.util.logging.Logger
import kotlin.math.roundToInt

class Simulation(
    val oransjePerDag: Int = 2,
    val gulePerDag: Int = 54,
    val gronnePerDag: Int = 12,
    val gjennomsittForLedigLege: Int = 18,
    val gjennomsittligIntervallAnkomst: Int = 13,
    val antallPasienter: Int = 50,
    val randomSeed: Int = 10,
    val algoritme: QueueAlgoritm = FOUR_STATUSES,
    val variansAnkomst: Double = 0.8,
    val variansBehandling: Double = 0.0,
    val status2Capacity: Int = 1,
    val status3Capacity: Int = 3
) {

    val log: Logger = Logger.getAnonymousLogger()
    val startTime = LocalTime.of(8, 0)

    fun runSimulation() {
        log.info(
            "Kjører med følgende parametre: \n" +
                    "Oransje per dag: $oransjePerDag \n" +
                    "Gule per dag: $gulePerDag \n" +
                    "Grønne per dag: $gronnePerDag \n" +
                    "Gjennomsittlig  tid før neste innkalling: $gjennomsittForLedigLege \n" +
                    "Gjennomsittlig intervall ankomst: $gjennomsittligIntervallAnkomst \n" +
                    "Antall entries: $antallPasienter \n" +
                    "Random seed: $randomSeed \n" +
                    "Algoritm: $algoritme \n" +
                    "Varians (ankomst og behandling): $variansAnkomst og $variansBehandling \n" +
                    "Kapasitet status2: $status2Capacity \n" +
                    "Kapasitet status3: $status3Capacity \n"
        )

        val entries = generateEntries(
            antallPasienter,
            variansAnkomst,
            randomSeed,
            oransjePerDag,
            gulePerDag,
            gronnePerDag,
            startTime,
            gjennomsittligIntervallAnkomst
        )
        val treatmentTimes =
            generateTreatmentTimes(entries.size, startTime, variansBehandling, gjennomsittForLedigLege, randomSeed)
        val events = treatPatients(entries, treatmentTimes, algoritme)

        log.info(getStats(events))

        log.info("Arrivals \n${events.filter { it.type == EventType.ARRIVAL }.joinToString("\n")}\n" +
                "Treatments: \n${events.filter { it.type == EventType.TREATMENT }.joinToString("\n")}"
        )
    }

    private fun treatPatients(
        entries: List<Entry>,
        treatmentTimes: List<LocalTime>,
        algoritm: QueueAlgoritm
    ): List<Event> {
        return when (algoritm) {
            FIFO -> treatPatientsInListsOrder(entries, treatmentTimes, startTime)
            TODAY -> treatPatientsInApproximatelyTodaysOrder(entries, treatmentTimes, startTime)
            FOUR_STATUSES -> treatPatientsInFourQueueCategories(entries, treatmentTimes)
        }
    }

    private fun treatPatientsInFourQueueCategories(
        entries: List<Entry>,
        treatmentTimes: List<LocalTime>): List<Event> {
        val status2Bag = mutableListOf<Entry>()
        val status3Bag = mutableListOf<Entry>()
        val status4Bag = mutableListOf<Entry>()
        val eventList = mutableListOf<Event>()

        val arrivalsAndTreatments: MutableList<Pair<LocalTime, EventType>> =
            treatmentTimes.map { it to EventType.TREATMENT }.toMutableList()
        arrivalsAndTreatments.addAll(entries.map { Pair(it.time, EventType.ARRIVAL) })
        arrivalsAndTreatments.sortBy { it.first }

        for (t in arrivalsAndTreatments) {
            val currentTime = t.first
            if (t.second == EventType.TREATMENT && treatmentTimes.firstOrNull { it.equals(currentTime) } != null) {
                var currentPatient: Entry? = null
                if (status2Bag.isEmpty()) {
                    eventList.add(Event(t.first, EventType.TREATMENT, "Ingen pasienter", null))
                } else {
                    currentPatient = status2Bag.removeAt(0)
                    if (status2Bag.size < status2Capacity && status3Bag.isNotEmpty()) {
                        status2Bag.add(status3Bag.removeAt(0))
                    }
                    if (status3Bag.size < status3Capacity && status4Bag.isNotEmpty()) {
                        status3Bag.add(status4Bag.removeAt(0))
                    }
                    val status2BagString = status2Bag.joinToString(",") { it.print(currentTime) }
                    val status3BagString = status3Bag.joinToString(",") { it.print(currentTime) }
                    val status4BagString = status4Bag.joinToString(",") { it.print(currentTime) }

                    eventList.add(
                        Event(
                            t.first,
                            EventType.TREATMENT,
                            "1️⃣:${currentPatient.print(currentTime)} 2️⃣: $status2BagString 3️⃣: $status3BagString 4️⃣: $status4BagString",
                            currentPatient
                        )
                    )
                }
            } else if (t.second == EventType.ARRIVAL && entries.map { it.time }
                    .firstOrNull { it.equals(currentTime) } != null) {
                val currentPatient =
                    entries.find { it.time.equals(currentTime) } ?: throw Exception("ERROR: No patient found")
                if (currentPatient.priority == Priority.HIGH || status2Bag.size < status2Capacity) {
                    status2Bag.add(currentPatient)
                } else if (currentPatient.priority == Priority.MEDIUM || status3Bag.size < status3Capacity) {
                    status3Bag.add(currentPatient)
                } else {
                    status4Bag.add(currentPatient)
                }
                val status2BagString = status2Bag.joinToString(",") { it.print(currentTime) }
                val status3BagString = status3Bag.joinToString(",") { it.print(currentTime) }
                val status4BagString = status4Bag.joinToString(",") { it.print(currentTime) }

                eventList.add(
                    Event(
                        currentTime,
                        EventType.ARRIVAL,
                        "2️⃣: $status2BagString 3️⃣: $status3BagString 4️⃣: $status4BagString",
                        currentPatient
                    )
                )
            }
        }
        return eventList
    }

    private fun treatPatientsInApproximatelyTodaysOrder(
        entries: List<Entry>,
        treatmentTimes: List<LocalTime>,
        startTime: LocalTime
    ): List<Event> {
        val minOrange = 2L
        val minYellow = 5L
        val minGreen = 15L
        val maxOrange = 15L
        val maxYellow = 45L
        val maxGreen = 120L

        var currentTime = startTime
        val listOfTreatments = mutableListOf<Event>()
        val orangePatients = entries.filter { it.priority == Priority.HIGH }.sortedBy { it.time }.toMutableList()
        val yellowPatients = entries.filter { it.priority == Priority.MEDIUM }.sortedBy { it.time }.toMutableList()
        val greenPatients = entries.filter { it.priority == Priority.LOW }.sortedBy { it.time }.toMutableList()

        for (i in treatmentTimes.indices) {
            var currentPatient: Entry? = null;
            val orangePatientLast =
                orangePatients.firstOrNull() { it.time.isBefore(currentTime.minusMinutes(maxOrange)) }
            val yellomPatientLast =
                yellowPatients.firstOrNull() { it.time.isBefore(currentTime.minusMinutes(maxYellow)) }
            val greenPatientLast = greenPatients.firstOrNull() { it.time.isBefore(currentTime.minusMinutes(maxGreen)) }
            val orangePatientFirst = orangePatients.firstOrNull {
                it.time.isBefore(currentTime) && it.time.isBefore(
                    currentTime.minusMinutes(minOrange)
                )
            }
            val yellomPatientFirst = yellowPatients.firstOrNull {
                it.time.isBefore(currentTime) && it.time.isBefore(
                    currentTime.minusMinutes(minYellow)
                )
            }
            val greenPatientFirst = greenPatients.firstOrNull {
                it.time.isBefore(currentTime) && it.time.isBefore(
                    currentTime.minusMinutes(minGreen)
                )
            }
            if (orangePatientLast != null) {
                currentPatient = orangePatientLast
                orangePatients.remove(orangePatientLast)
            } else if (yellomPatientLast != null) {
                currentPatient = yellomPatientLast
                yellowPatients.remove(yellomPatientLast)
            } else if (greenPatientLast != null) {
                currentPatient = greenPatientLast
                greenPatients.remove(greenPatientLast)
            } else if (orangePatientFirst != null) {
                currentPatient = orangePatientFirst
                orangePatients.remove(orangePatientFirst)
            } else if (yellomPatientFirst != null) {
                currentPatient = yellomPatientFirst
                yellowPatients.remove(yellomPatientFirst)
            } else if (greenPatientFirst != null) {
                currentPatient = greenPatientFirst
                greenPatients.remove(greenPatientFirst)
            } else if (orangePatients.filter { it.time.isBefore(currentTime) }.isNotEmpty()) {
                currentPatient = orangePatients.removeFirst()
            } else if (yellowPatients.filter { it.time.isBefore(currentTime) }.isNotEmpty()) {
                currentPatient = yellowPatients.removeFirst()
            } else if (greenPatients.filter { it.time.isBefore(currentTime) }.isNotEmpty()) {
                currentPatient = greenPatients.removeFirst()
            }
            if (currentPatient == null) {
                listOfTreatments.add(Event(treatmentTimes[i], EventType.TREATMENT, "Ingen pasienter", null))
            } else {
                listOfTreatments.add(Event(currentTime, EventType.TREATMENT, "${currentPatient.print(currentTime)}${
                    orangePatients.filter { it.time.isBefore(currentTime) }.map { it.print(currentTime) }
                        .joinToString(",", ",")
                }${
                    yellowPatients.filter { it.time.isBefore(currentTime) }.map { it.print(currentTime) }
                        .joinToString(",", ",")
                }${
                    greenPatients.filter { it.time.isBefore(currentTime) }.map { it.print(currentTime) }
                        .joinToString(",", ",")
                }", currentPatient
                )
                )
            }
            currentTime = treatmentTimes[i]
        }
        return listOfTreatments
    }

    private fun treatPatientsInListsOrder(
        entries: List<Entry>,
        treatmentTimes: List<LocalTime>,
        startTime: LocalTime
    ): List<Event> {
        var currentTime = startTime
        var listOfTreatments = mutableListOf<Event>()
        var remainingPatients = entries.toMutableList()
        for (element in treatmentTimes) {
            val currentPatient = remainingPatients.firstOrNull { it.time.isBefore(currentTime) }
            listOfTreatments.add(
                Event(currentTime, EventType.TREATMENT, "${
                    remainingPatients.slice(0 until remainingPatients.size).filter { it.time.isBefore(currentTime) }
                        .map { it.print(currentTime) }.joinToString(",")
                }", currentPatient
                )
            )
            currentTime = element
            remainingPatients = remainingPatients.subList(1, remainingPatients.size)
        }
        return listOfTreatments
    }
}


private fun getStats(events: List<Event>): String {
    val orange = events.filter { it.type == EventType.TREATMENT && it.patient?.priority == Priority.HIGH }
        .map { Duration.between(it.patient?.time, it.time).toMinutes() }
    val yellow = events.filter { it.type == EventType.TREATMENT && it.patient?.priority == Priority.MEDIUM }
        .map { Duration.between(it.patient?.time, it.time).toMinutes() }
    val green = events.filter { it.type == EventType.TREATMENT && it.patient?.priority == Priority.LOW }
        .map { Duration.between(it.patient?.time, it.time).toMinutes() }

    val orangeString = "Oransje: max=${orange.maxOrNull()} gjsnitt=${orange.average()}"
    val yellowString = "Gul    : max=${yellow.maxOrNull()} gjsnitt=${yellow.average()}"
    val greenString = "Grønn  : max=${green.maxOrNull()} gjsnitt=${green.average()}"
    return "\n$orangeString \n$yellowString \n$greenString"
}


fun Entry.print(now: LocalTime): String {
    val timeDiff =
        ((now.toSecondOfDay() - time.toSecondOfDay()).toDouble() / 60).roundToInt().toString().padStart(3, ' ')

    return "[${quenr.padStart(2, ' ')} $priority ${timeDiff}m]"
}

