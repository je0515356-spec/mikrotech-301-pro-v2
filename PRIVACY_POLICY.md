# Politique de Confidentialité — MIKROTECH 301 PRO V2

**Dernière mise à jour : 10 Novembre 2026**

L'application mobile **MIKROTECH 301 PRO V2** (ci-après « l'Application »), développée par **Jean Eric** (`je0515356@gmail.com`), respecte scrupuleusement la vie privée de ses utilisateurs.

---

### 1. Absence de Collecte de Données Personnelles
L'Application fonctionne **exclusivement en réseau local (LAN/Wi-Fi)** directement entre votre smartphone Android et vos routeurs MikroTik (RouterOS v6 & v7).
- Aucune donnée personnelle, identifiant, mot de passe de routeur ou ticket n'est envoyé vers des serveurs tiers ou vers le cloud.
- Aucun traceur publicitaire, outil d'analyse comportementale ou SDK de télémétrie n'est intégré.

### 2. Données Stockées Localement sur Votre Appareil
Les informations suivantes restent stockées de manière strictement locale et chiffrée sur la mémoire interne de votre smartphone Android :
- Adresses IP locales et identifiants administrateur de vos routeurs enregistrés (pour la reconnexion automatique).
- Historique de génération des tickets et suivi comptable des ventes de tickets.
- Paramètres d'impression et nom personnalisé de votre zone Wi-Fi.

Ces données ne sont jamais partagées et sont immédiatement supprimées si vous désinstallez l'Application.

### 3. Autorisations Android Requises
L'Application demande uniquement les permissions strictement nécessaires à ses fonctionnalités techniques :
- **Réseau local & Wi-Fi** (`INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`) : pour communiquer directement avec les routeurs MikroTik sur le réseau local et détecter les équipements.
- **Impression** : pour envoyer les tickets vers le service d'impression Android (`PrintManager`) ou imprimantes thermiques Bluetooth / Réseau.
- **Stockage / Téléchargements** : pour exporter les planches PDF A4 ou les images de tickets dans le dossier de téléchargement de l'utilisateur.
- **Notifications** : pour vous alerter en cas de nouvelle connexion client sur la cloche de notification ou de mise à jour RouterOS disponible.

### 4. Sécurité
Les communications entre l'Application et vos routeurs MikroTik utilisent l'API native RouterOS ou l'API REST HTTPS/HTTP sécurisée sur votre réseau local privé. Les sauvegardes RouterOS (`.backup`) créées lors des mises à jour système restent stockées sur la mémoire interne de votre propre routeur MikroTik.

### 5. Contact du Développeur
Pour toute question concernant cette politique de confidentialité ou le fonctionnement de l'application :
- **Développeur** : Jean Eric
- **Email** : `je0515356@gmail.com`
- **Assistance / WhatsApp** : `+261 38 28 171 00` (038 28 171 00)
- **Droits** : Tous droits réservés © 2026
