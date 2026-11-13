# MIKROTECH 301 ULTIMATE PRO V2

**Votre réseau. Votre business. Votre contrôle.**  
Signature commerciale : **MIKROTECH 301 — Madagascar**  
Contact développeur & assistance commerciale : **038 28 171 00**

---

## 1. Présentation générale

**MIKROTECH 301 ULTIMATE PRO V2** est une application Android native complète, développée en **Kotlin** et **Jetpack Compose**, conçue spécifiquement pour la commercialisation et l'administration des routeurs **MikroTik** (notamment *hAP ac²*, *hAP ax²*, *RouterBOARD*, *Cloud Router Switch*) à Madagascar et à l'international.

L'application permet aux exploitants de zones Wi-Fi (Hotspot) et aux fournisseurs d'accès Internet (PPPoE) de gérer en temps réel leurs équipements, de créer et vendre des tickets en **Ariary (Ar)**, d'éjecter des sessions, de personnaliser leur portail captif et d'exporter leur comptabilité financière sans dépendre d'un serveur cloud externe.

---

## 2. Fonctionnalités majeures

### 📊 Dashboard Haute Fidélité
- Télémétrie en temps réel du routeur : Modèle, version RouterOS, Uptime, fréquence processeur, jauge dynamique de charge CPU, pourcentage d'utilisation mémoire RAM, consommation disque flash.
- Compteurs en direct : Clients Hotspot connectés, sessions PPPoE actives, total clients actifs.
- Billeterie : Tickets actifs, tickets expirés/suspendus, tickets disponibles.
- Statistiques de ventes du jour et revenus du mois en **Ariary (Ar)**.
- Graphique interactif de revenus des 7 derniers jours et répartition par mode de paiement.
- Raccourcis d'actions rapides (Nouveau ticket, Générer un lot, Client PPPoE, Profils, Routeurs).

### 🎟️ Gestion Complète des Tickets Hotspot
- **Création unitaire** avec générateur aléatoire d'identifiant et de code PIN sécurisé.
- **Génération en lot (Batch)** de 10, 20, 50 ou 100 tickets avec préfixe personnalisable.
- Préréglages de durée : 1h, 3h, 12h, 24h, 3 jours, 7 jours, 30 jours, ou personnalisée.
- Préréglages de prix en Ariary (500 Ar, 1 000 Ar, 2 000 Ar, 2 500 Ar, 5 000 Ar, 10 000 Ar).
- **Voucher Visuel Imprimable** (conforme formats ticket de caisse thermique Bluetooth 58 mm et 80 mm) avec QR Code, coordonnées vendeur, tarifs, identifiant monospace en grand format.
- Partage direct WhatsApp et partage système Android.
- Modification en direct sur RouterOS (nom d'utilisateur, mot de passe, profil, temps limite, suspension/réactivation).
- Réinitialisation des compteurs d'octets et de session.
- Suppression avec boîte de dialogue de confirmation sécurisée.

### 👥 Gestion des Clients Connectés
- **Onglet HOTSPOT** : Nom d'utilisateur, adresse IP, adresse MAC, durée de session (uptime), trafic consommé (RX/TX), méthode de connexion, bouton d'éjection immédiate.
- **Onglet PPPoE** : Nom d'utilisateur, adresse IP assignée, Caller ID (MAC), uptime, service, déconnexion de session active.
- Filtrage instantané par saisie texte (nom, IP, MAC).

### 📈 Comptabilité & Ventes (Ariary)
- Journal complet des ventes avec horodatage, opérateur, référence et montant en Ariary.
- Prise en charge des modes de règlement : **Espèces**, **MVola**, **Orange Money**, **Airtel Money**.
- Filtrage interactif par mode de paiement.
- Annulation traçable de vente avec motif pour éviter les discordances comptables.
- **Export CSV** vers Excel ou messagerie pour le bilan comptable.

### ⚡ Gestion PPPoE Professionnelle
- Liste des secrets PPPoE créés sur le MikroTik.
- Ajout de client PPPoE avec fiche commerciale locale (nom client, téléphone, adresse, tarif mensuel en Ariary, date d'échéance et renouvellement).
- Profils de débits préconfigurés : 2 Mbps, 5 Mbps, 10 Mbps, 15 Mbps, 20 Mbps et débit sur-mesure.

### 🌐 Portail Captif Personnalisable
- Personnalisation de la zone Wi-Fi : Nom de la zone, téléphone du vendeur, numéro WhatsApp, page Facebook, message d'accueil.
- 3 thèmes intégrés : **Dark Premium**, **Business Blue**, **Clean Light**.
- Aperçu en direct du rendu sur smartphone.
- Structure prête pour exportation des fichiers HTML RouterOS (`login.html`, `status.html`, `alogin.html`).

### ⚙️ Multi-Routeurs & Connexion MikroTik
- Prise en charge des protocoles :
  1. **API Socket binaire RouterOS** (port standard 8728 ou API-SSL 8729) compatible RouterOS v6 et v7.
  2. **REST API HTTPS / HTTP** (port standard 443 ou 80) compatible RouterOS v7.1+.
- Découverte automatique des équipements MikroTik sur le réseau local (scan subnet 192.168.88.1, etc.).
- Gestion de plusieurs routeurs enregistrés avec basculement rapide.
- **Mode Démonstration complet intégré** pour former les revendeurs et tester toutes les fonctionnalités hors ligne.

### 🛡️ Maintenance & Sécurité
- Diagnostic matériel complet.
- Génération de fichier de sauvegarde MikroTik (`.backup`).
- Redémarrage sécurisé du routeur avec double confirmation explicite.
- Architecture de licence commerciale intégrée (**ULTIMATE PRO**).

---

## 3. Architecture Technique

- **Langage** : Kotlin 100%
- **Interface Utilisateur** : Jetpack Compose & Material 3 (Palette fintech Dark Premium : `#080F1F`, `#111D33`, `#387BFF`, `#00D9F5`, `#12D69C`, `#FF5265`)
- **Architecture** : MVVM (Model - View - ViewModel) + Repository Pattern
- **Base de Données Locale** : Android Room Database avec KSP
- **Réseau** : OkHttp 4, Socket TCP binaire RouterOS, Coroutines & Flow
- **Tests** : JUnit 4 & Robolectric

---

## 4. Instructions de Compilation & Génération APK

Pour compiler l'application et générer l'APK de débogage ou de production :

### Compilation de l'APK Debug :
```bash
gradle :app:assembleDebug
```
L'APK généré sera disponible dans :
`app/build/outputs/apk/debug/app-debug.apk`

### Exécution des tests unitaires et Robolectric :
```bash
gradle :app:testDebugUnitTest
```

---

## 5. Contact & Support

Développé par **MIKROTECH 301**  
Madagascar  
Téléphone / WhatsApp : **038 28 171 00**  
Slogan : *Votre réseau. Votre business. Votre contrôle.*
