package com.shukranibusiness.app.util

object CsvImporter {

    data class ParsedProduct(
        val name: String,
        val category: String,
        val priceCdf: Double,
        val priceUsd: Double,
        val quantity: Int,
        val lowStockThreshold: Int,
        val purchasePriceCdf: Double,
        val purchasePriceUsd: Double
    )

    data class ImportResult(val parsed: List<ParsedProduct>, val skippedLines: Int)

    /**
     * Colonnes attendues (avec en-tête, ignorée) :
     * Nom, Catégorie, Prix CDF, Prix USD, Quantité, Seuil stock bas, Prix d'achat CDF, Prix d'achat USD
     */
    fun parse(text: String): ImportResult {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return ImportResult(emptyList(), 0)

        val parsed = mutableListOf<ParsedProduct>()
        var skipped = 0
        for (line in lines.drop(1)) {
            val fields = parseCsvLine(line)
            val name = fields.getOrNull(0)?.trim().orEmpty()
            if (name.isEmpty()) {
                skipped++
                continue
            }
            parsed += ParsedProduct(
                name = name,
                category = fields.getOrNull(1)?.trim()?.ifEmpty { "Général" } ?: "Général",
                priceCdf = fields.getOrNull(2)?.trim()?.toDoubleOrNull() ?: 0.0,
                priceUsd = fields.getOrNull(3)?.trim()?.toDoubleOrNull() ?: 0.0,
                quantity = fields.getOrNull(4)?.trim()?.toIntOrNull() ?: 0,
                lowStockThreshold = fields.getOrNull(5)?.trim()?.toIntOrNull() ?: 5,
                purchasePriceCdf = fields.getOrNull(6)?.trim()?.toDoubleOrNull() ?: 0.0,
                purchasePriceUsd = fields.getOrNull(7)?.trim()?.toDoubleOrNull() ?: 0.0
            )
        }
        return ImportResult(parsed, skipped)
    }

    /** Parseur CSV simple : gère les champs entre guillemets (avec virgules ou guillemets échappés). */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result += current.toString()
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result += current.toString()
        return result
    }
}
