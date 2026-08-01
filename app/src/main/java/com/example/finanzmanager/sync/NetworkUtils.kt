package com.example.finanzmanager.sync

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

/** Lokale IPv4-Adressen dieses Geräts (ohne Loopback) – Taxologic zeigt das Pendant
 * dazu bereits an (sync.rs::lokale_ipv4_adressen), damit sich der Nutzer die Adresse
 * nicht in den Android-Netzwerkeinstellungen zusammensuchen muss.
 *
 * Das Gerät kann mehrere aktive Schnittstellen gleichzeitig haben (WLAN, VPN,
 * virtuelle Adapter...) – ohne Priorisierung landet man leicht bei der falschen
 * IP. Das eigentliche WLAN-Interface heißt auf so gut wie jedem Android-Gerät
 * "wlan0", daher wird das hier gezielt zuerst einsortiert. */
fun lokaleIpAdressen(): List<String> = try {
    Collections.list(NetworkInterface.getNetworkInterfaces())
        .filter { !it.isLoopback && it.isUp }
        .sortedByDescending { it.name.startsWith("wlan") }
        .flatMap { Collections.list(it.inetAddresses) }
        .filterIsInstance<Inet4Address>()
        .map { it.hostAddress }
        .filterNotNull()
} catch (e: Exception) {
    emptyList()
}
