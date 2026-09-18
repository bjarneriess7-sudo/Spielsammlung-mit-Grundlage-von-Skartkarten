# Spielsammlung-mit-Grundlage-von-Skartkarten

Hier ist der genaue Ablaufplan für alle 4 Gruppenmitglieder. Da du deinen Code bereits als Fundament bereitgestellt hast, weiß jetzt jeder exakt, was er zu tun hat:

---

### Person 1 (Oskar): Core-Architektur, Sicherheit & Basis-Dokumentation

* **Deine erledigten Code-Aufgaben:**
* Basisklassen & Datenmodelle: `Suit.java`, `Rank.java`, `Card.java`, `Deck.java`, `Hand.java`.


* Algorithmen & Muster: Fisher-Yates-Mischverfahren, Sortier-Comparatoren (`CardComparators.java`).


* Fehlerbehandlung: `EmptyDeckException.java`, `InvalidCardException.java`.


* Sicherheit & Persistenz: `UserAccount.java` (SHA-256 Hashing), `SecurityService.java`, `AccountRepository.java` (CSV-Speicherung).


* Testsuite: `CoreTestRunner.java` mit 15 bestandenen Tests.

---

### Person 2: Das Geduldspiel-Modul (Solitär / Klondike)

* **Code-Aufgabe:**
* Baut das Solitär-Spiel auf Basis deiner Klassen `Card`, `Deck` und `Hand`.


* Erstellt die Klassen:
* `SolitaireColumn.java` (die 7 Spielspalten mit verdeckten und offenen Karten).
* `SolitaireFoundation.java` (die 4 Zielfelder von Ass bis König).
* `SolitaireMove.java` (führt Züge aus und speichert Historie für Undo).
* `SolitaireGame.java` (implementiert das Interface `PlayableGame` / `GameCoreInterfaces`).


* `SolitaireTestRunner.java` (eigene Tests für Kartenzüge, Stapeln und Siegbedingungen).

---

### Person 3: Das Mehrspieler- & KI-Modul (Blackjack)

* **Code-Aufgabe:**
* Baut das Blackjack-Spiel inklusive eines automatisierten Computer-Dealers (Bot).
* Erstellt die Klassen:
* `BlackjackPlayer.java` (implementiert das `Player`-Interface; verwaltet Einsätze und Handkarten).


* `BlackjackDealerAi.java` (KI-Logik des Dealers: Zieht Karten nach festen Wahrscheinlichkeiten / bis mindestens 17).
* `BettingSystem.java` (Verwaltung von Spielguthaben, Mindest- und Höchsteinsätzen).
* `BlackjackGame.java` (implementiert `PlayableGame`, steuert Rundenablauf und Gewinnausschüttung).


* `BlackjackTestRunner.java` (Tests für Dealer-Entscheidungen, Split/Double-Down und Ass-Wertung).

---

### Person 4: UI, Gesamtintegration & Redaktionsleitung

* **Code-Aufgabe:**
* Führt alle Module in einem gemeinsamen Hauptprogramm zusammen.
* Erstellt die Klassen:
* `ConsoleUI.java` (Menüausgabe auf der Konsole, Textformatierung, geschützte Scannereingaben).
* `Main.java` (zentraler Einstieg: Startmenü -> Registrierung/Login über `SecurityService` -> Spielauswahl -> Punkte ins Profil speichern via `AccountRepository`).


* `ProjectWordCounter.java` (ein kleines Skript, das alle `.java`-Dateien einliest und die genaue Wortanzahl für die Titelseite ausgibt).


* `IntegrationTest.java` (testet den kompletten Durchlauf vom Login bis zum Spielende).


Person 2 und 3 öffnen einfach Gemini, laden deine ZIP-Datei hoch und fordern genau ihre 5 Klassen an. Sobald deren Klassen fertig sind, kann Person 4 die `Main.java` schreiben und alles zu einem spielbaren Komplettprogramm verknüpfen.
