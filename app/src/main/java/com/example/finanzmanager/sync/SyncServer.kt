package com.example.finanzmanager.sync

import android.content.Context
import com.example.finanzmanager.data.database.AppDatabase
import com.example.finanzmanager.data.repository.FinanzRepository
import com.example.finanzmanager.data.repository.SettingsRepository
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Lokaler, rein lesender HTTP-Server für den Taxologic-Sync: liefert auf Anfrage
 * den bestehenden JSON-Export (siehe FinanzRepository.exportToJson) aus. Taxologic
 * holt sich die Daten aktiv ab (Pull) – dieser Server initiiert selbst nie etwas
 * und schreibt nie in die eigene Datenbank zurück. Läuft dauerhaft im Hintergrund,
 * solange der App-Prozess lebt (gestartet in FinanzManagerApp.onCreate).
 *
 * Ein Pairing-Code (X-Finanzmanager-Token-Header) schützt vor Zugriffen durch
 * andere Geräte/Apps im selben WLAN – Taxologic zeigt denselben Mechanismus
 * bereits für seinen eigenen Geräte-Sync.
 */
/** Sichtbarer Start-Status für die Sync-Section in den Einstellungen – ohne das
 * würde ein Startfehler nur in Logcat landen und für den Nutzer unsichtbar bleiben. */
object SyncServerStatus {
    @Volatile var running: Boolean = false
    @Volatile var fehler: String? = null
}

class SyncServer(private val context: Context) : NanoHTTPD("0.0.0.0", PORT) {

    private val settings = SettingsRepository(context)

    override fun serve(session: IHTTPSession): Response {
        return when (session.uri) {
            "/finanzmanager-sync/status" -> {
                if (!tokenGueltig(session)) return unauthorized()
                jsonResponse("""{"app":"finanzmanager"}""")
            }
            "/finanzmanager-sync/export" -> {
                if (!tokenGueltig(session)) return unauthorized()
                jsonResponse(runBlocking { exportJson() })
            }
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
        }
    }

    private fun tokenGueltig(session: IHTTPSession): Boolean {
        val eingehend = session.headers["x-finanzmanager-token"]?.trim()?.uppercase() ?: ""
        if (eingehend.isEmpty()) return false
        val lokal = runBlocking { settings.syncToken.first() }
        return eingehend == lokal
    }

    private suspend fun exportJson(): String {
        val repo = FinanzRepository(AppDatabase.getDatabase(context))
        return repo.exportToJson(
            accounts = repo.accounts.first(),
            transactions = repo.transactions.first(),
            categories = repo.categories.first(),
            standingOrders = repo.standingOrders.first(),
            settings = emptyMap(),
        )
    }

    private fun jsonResponse(json: String) =
        newFixedLengthResponse(Response.Status.OK, "application/json", json)

    private fun unauthorized() =
        newFixedLengthResponse(Response.Status.UNAUTHORIZED, "text/plain", "Unauthorized")

    companion object {
        const val PORT = 47823
    }
}
