package com.example.demo

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class Entry(val quenr: String, val priority: Priority, val time: LocalTime) {
    override fun toString(): String {
        return "$quenr $priority $time"
    }
}

data class Event(val time: LocalTime, val type: EventType, val event: String, val patient: Entry?) {
    val hhMM = DateTimeFormatter.ofPattern("HH:mm")

    override fun toString(): String {
        return "${time.format(hhMM)} $type $event"
    }
}

enum class EventType {
    ARRIVAL, TREATMENT
}

enum class Priority {
    HIGH, MEDIUM, LOW;

    override fun toString(): String {
        return when (this) {
            HIGH -> "🟠"
            MEDIUM -> "🟡"
            LOW -> "🟢"
        }
    }
}

enum class QueueAlgoritm {
    FIFO, FOUR_STATUSES, TODAY;

    override fun toString(): String {
        return when(this) {
            FIFO -> "Ingen prioritering"
            FOUR_STATUSES -> "Foreslått algoritme"
            TODAY -> "Dagens"
        }
    }
}