package com.thehan.dailyspeak.domain.model

/**
 * User-selected practice level. Individual questions are always DAILY or POSTGRADUATE;
 * MIXED means that both pools should be considered.
 */
enum class PracticeLevel {
    DAILY,
    POSTGRADUATE,
    MIXED,
}