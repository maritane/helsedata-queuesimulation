package com.example.demo

import java.time.LocalTime
import kotlin.math.ln
import kotlin.random.Random


fun generateEntries(antallEntries: Int, variansAnkomst: Double, randomSeed: Int, oransjePerDag: Int, gulePerDag: Int, gronnePerDag: Int, startTime: LocalTime, gjennomsittligIntervallAnkomst: Int): List<Entry> {
    val entries: MutableList<Entry> = mutableListOf()
    val intervals = generatePoissonIntervals(antallEntries, variansAnkomst, randomSeed)
    val priorities = generatePriorities(antallEntries, oransjePerDag, gulePerDag, gronnePerDag, randomSeed)
    var time = startTime

    for (i in 0 until antallEntries) {
        time = time.plusSeconds(gjennomsittligIntervallAnkomst*60 + (intervals[i] * gjennomsittligIntervallAnkomst*60).toLong())
        val entry = Entry("A" + (i + 10).toString(), priorities[i], time)
        entries.add(entry)
    }
    return entries
}


fun generatePriorities(antallEntries: Int, orange: Int, gule: Int, gronne: Int, randomSeed: Int): List<Priority> {
    val random = Random(randomSeed)
    val sumOrangeGuleGronne = orange + gule + gronne
    val priorities = mutableListOf<Priority>()
    var sum = 0
    while (sum < antallEntries) {
        val u = random.nextDouble() * sumOrangeGuleGronne
        if (u < orange) {
            priorities.add(Priority.HIGH)
        } else if (u < orange + gule) {
            priorities.add(Priority.MEDIUM)
        } else {
            priorities.add(Priority.LOW)
        }
        sum += 1
    }
    return priorities
}


fun generateTreatmentTimes(numberOfTreatments: Int, startTime: LocalTime, varians: Double, gjennomsittForLedigLege: Int, randomSeed: Int): List<LocalTime> {
    var currentTime = startTime
    val intervals = generatePoissonIntervals(numberOfTreatments, varians, randomSeed + 1)
    val treatmentTimes = mutableListOf<LocalTime>()
    for (i in 0 until numberOfTreatments) {
        currentTime = currentTime.plusSeconds(gjennomsittForLedigLege*60 + (intervals[i] * gjennomsittForLedigLege * 60).toLong())
        if (currentTime > LocalTime.of(23, 59))
            return treatmentTimes
        treatmentTimes.add(currentTime)
    }
    return treatmentTimes
}


fun generatePoissonIntervals(numEvents: Int, variance: Double, randomSeed: Int): List<Double> {
    val lambda = numEvents.toDouble() // lambda = expected number of events
    val random = Random(randomSeed)
    val intervals = mutableListOf<Double>()
    var sum = 0.0
    while (sum < numEvents) {
        val u = random.nextDouble() // generate uniform random number between 0 and 1
        val x = (-ln(u) / lambda) + variance * (random.nextDouble() * 2 - 1) // generate Poisson-distributed interval with variance
        sum += 1
        intervals.add(x)
    }
    return intervals
}