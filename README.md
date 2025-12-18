# Intercom2SOS — Survival Pack (Android)

**Intercom2SOS** est une application Android orientée “survival pack” : communication **Intercom**, assistance **SOS** en situation critique (pronostic vital engagé), et compatibilité renforcée avec l’écosystème **Xiaomi** (téléphones + bague/capteur BLE selon configuration).

> Objectif: fournir une **procédure simple et rapide** pour déclencher une alerte, partager une position GPS, et (si présent) émettre un message d’urgence via un module radio/USB.

---

## Fonctionnalités principales

### 1) Mode Intercom (PTT)
- Contrôle **PTT (Push-To-Talk)** via un module connecté en **USB** (mode host).
- Commandes envoyées au contrôleur USB (ex: fréquence, état, PTT).

### 2) Mode SOS (urgence vitale)
- Récupération de la **position GPS** (haute précision si disponible).
- Génération d’un **message d’urgence** (texte partageable).
- Option “émission” via PTT + message vocal (TTS) si le module radio USB est connecté.
- Bouton d’appel urgences via le dialer (**112** ou **911** selon pays).

### 3) “Survival Pack” (workflow)
- Une interface pensée pour:
  - Vérifier rapidement l’état (USB connecté / BLE connecté / GPS OK)
  - Déclencher un SOS en mode **TEST** ou en mode **réel**
  - Partager un message d’urgence en 2-3 actions

### 4) Compatibilité Xiaomi / “Ring”
- Support BLE (ex: capteur type bague) pour remonter la **fréquence cardiaque** (bpm) si disponible.
- L’info bpm peut être intégrée dans le message SOS (selon implémentation).

---

## Procédure recommandée (Survival Pack)

### Mode TEST (recommandé avant usage réel)
1. Activer **Localisation** (GPS).
2. Connecter le module **USB** (si utilisé).
3. (Optionnel) Connecter le capteur **BLE**.
4. Lancer **SOS en mode TEST**:
   - Vérifie GPS
   - Envoie les infos (sans émission réelle)

### Mode SOS réel (urgence)
1. Vérifier que le **module USB** est connecté (si tu utilises la partie radio/émission).
2. Déclencher **SOS**.
3. Optionnel:
   - **Appeler** les urgences (112/911)
   - **Partager** le message d’urgence via SMS/WhatsApp/etc.

---

## Permissions & vie privée

L’application peut utiliser:
- **Localisation** (GPS) pour le SOS / partage
- **Bluetooth (BLE)** pour capteur (bague)
- **Notifications** pour service au premier plan (USB)
- **Micro / audio** selon fonctionnalités intercom

Les données (GPS, bpm) sont utilisées pour l’affichage et/ou le message d’urgence.  
**Ne pas utiliser en production sans audit sécurité** si tu ajoutes automatisation (appel/SMS).

---

## Stack technique
- Kotlin / Android
- Jetpack Compose + (parties XML possibles)
- Hilt (DI)
- Google Play Services Location
- BLE (GATT HR service)
- USB host + sérialisation (usb-serial-for-android)

---

## Avertissement (important)
Ce projet touche aux **scénarios d’urgence**.  
Avant toute diffusion ou utilisation réelle:
- Tester le mode TEST
- Respecter les lois locales (urgence, appels, automatisation)
- Vérifier contraintes Android (permissions runtime, restrictions background/foreground)

---

## Build / Run
1. Ouvrir dans Android Studio
2. Sync Gradle
3. Lancer sur un appareil (permissions nécessaires)
4. Tester d’abord le **mode TEST**

---

## Roadmap (idées)
- Ajout d’un écran “Checklist Survival Pack” (avant SOS)
- Journal d’événements (GPS acquis, USB ok, émission ok)
- Gestion runtime propre de `POST_NOTIFICATIONS` (Android 13+)
- Profils pays: 112/911 + texte localisé
