package com.example.finanzmanager.ocr

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Ergebnis der Belegerkennung. Alle Felder sind optional – der Nutzer prüft
 * und korrigiert im Buchungsformular, bevor gespeichert wird.
 */
data class ScannedReceipt(
    val amount: Double?,      // erkannter Gesamtbetrag
    val date: String?,        // "yyyy-MM-dd"
    val merchant: String?,    // Händlername (oberste sinnvolle Zeile)
    val rawText: String       // kompletter erkannter Text (für Debug/Anzeige)
)

/**
 * Heuristischer Parser für deutsche Kassenbons / Rechnungen.
 *
 * Bewusst regelbasiert und ohne Netzwerk – läuft komplett offline und ergänzt
 * nur die On-Device-OCR (ML Kit). Da OCR nie perfekt ist, versteht sich das
 * Ergebnis als Vorschlag, den der Nutzer im Formular bestätigt.
 */
object ReceiptParser {

    // Geldbetrag im deutschen Format: 12,34  ·  1.234,56  ·  1 234,56
    private val MONEY = Regex("""(?<!\d)(\d{1,3}(?:[.\s]\d{3})*|\d+)[,.](\d{2})(?!\d)""")

    // Datum: 05.07.2026 · 5.7.26 · 05-07-2026 · 2026-07-05
    private val DATE_DMY = Regex("""(?<!\d)(\d{1,2})[.\-/](\d{1,2})[.\-/](\d{2,4})(?!\d)""")
    private val DATE_YMD = Regex("""(?<!\d)(\d{4})[.\-/](\d{1,2})[.\-/](\d{1,2})(?!\d)""")

    // Zeilen mit einem dieser Wörter enthalten mit hoher Wahrscheinlichkeit den Endbetrag.
    private val TOTAL_KEYWORDS = listOf(
        "summe", "gesamt", "gesamtbetrag", "total", "zu zahlen", "zahlbetrag",
        "endbetrag", "betrag", "rechnungsbetrag", "brutto"
    )

    // Zahlarten – auch hier steht oft der Endbetrag, aber schwächer gewichtet.
    private val PAYMENT_KEYWORDS = listOf(
        "bar", "kartenzahlung", "karte", "ec", "girocard", "kreditkarte",
        "visa", "mastercard", "paypal", "kontaktlos"
    )

    // Zeilen, die NICHT der Endbetrag sind (Zwischenwerte).
    private val NEGATIVE_KEYWORDS = listOf(
        "rückgeld", "rueckgeld", "gegeben", "zurück", "zurueck",
        "steuer", "mwst", "ust", "netto", "trinkgeld"
    )

    fun parse(rawText: String): ScannedReceipt {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return ScannedReceipt(
            amount = extractAmount(lines),
            date = extractDate(rawText),
            merchant = extractMerchant(lines),
            rawText = rawText
        )
    }

    private data class MoneyHit(val value: Double, val score: Int)

    private fun extractAmount(lines: List<String>): Double? {
        val hits = mutableListOf<MoneyHit>()

        for (line in lines) {
            val lower = line.lowercase()
            val values = MONEY.findAll(line).mapNotNull { moneyToDouble(it) }.toList()
            if (values.isEmpty()) continue

            val hasTotal = TOTAL_KEYWORDS.any { lower.contains(it) }
            val hasPayment = PAYMENT_KEYWORDS.any { lower.contains(it) }
            val hasNegative = NEGATIVE_KEYWORDS.any { lower.contains(it) }

            val score = when {
                hasNegative -> -5
                hasTotal    -> 10
                hasPayment  -> 6
                lower.contains("eur") || line.contains("€") -> 2
                else        -> 0
            }
            // Bei Total-/Zahlzeilen ist der größte Wert der Zeile der relevante Betrag.
            values.forEach { hits.add(MoneyHit(it, score)) }
        }

        if (hits.isEmpty()) return null

        val bestScore = hits.maxOf { it.score }
        // Unter mehreren Kandidaten gleicher Priorität den höchsten Betrag wählen
        // (Endbetrag ist typischerweise der größte positive Wert).
        return hits.filter { it.score == bestScore && it.score >= 0 }
            .maxByOrNull { it.value }?.value
            ?: hits.filter { it.score >= 0 }.maxByOrNull { it.value }?.value
    }

    private fun moneyToDouble(m: MatchResult): Double? {
        val intPart = m.groupValues[1].replace(".", "").replace(" ", "").replace(" ", "")
        val decPart = m.groupValues[2]
        return "$intPart.$decPart".toDoubleOrNull()
    }

    private fun extractDate(text: String): String? {
        DATE_YMD.find(text)?.let { m ->
            val (y, mo, d) = m.destructured
            buildDate(y.toInt(), mo.toInt(), d.toInt())?.let { return it }
        }
        // Alle DMY-Treffer prüfen, ersten gültigen nehmen.
        for (m in DATE_DMY.findAll(text)) {
            val (d, mo, yRaw) = m.destructured
            val year = normalizeYear(yRaw.toInt())
            buildDate(year, mo.toInt(), d.toInt())?.let { return it }
        }
        return null
    }

    private fun normalizeYear(y: Int): Int = when {
        y in 0..69   -> 2000 + y
        y in 70..99  -> 1900 + y
        else         -> y
    }

    private fun buildDate(year: Int, month: Int, day: Int): String? {
        return try {
            val date = LocalDate.of(year, month, day)
            // Zukunftsdaten (mehr als ein Tag) sind meist Fehlerkennungen.
            if (date.isAfter(LocalDate.now().plusDays(1))) return null
            date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (e: Exception) {
            null
        }
    }

    private fun extractMerchant(lines: List<String>): String? {
        // Erste "textige" Zeile im Kopf des Belegs: enthält Buchstaben,
        // ist nicht überwiegend aus Ziffern/Symbolen und kein Datum/Betrag.
        return lines.take(6).firstOrNull { line ->
            val letters = line.count { it.isLetter() }
            letters >= 3 &&
                letters >= line.count { it.isDigit() } &&
                !MONEY.containsMatchIn(line) &&
                !DATE_DMY.containsMatchIn(line) &&
                !line.lowercase().let { l -> TOTAL_KEYWORDS.any { l.contains(it) } }
        }?.take(40)
    }
}
