# Étape sécurité V2 — ne pas déployer avant configuration

Le code de la branche sépare désormais les droits **administrateur** et **routeur**.

- `ADMIN_API_TOKEN` (secret existant) : seulement `POST /queue` et `GET /result/{jobId}`.
- `ROUTER_TOKENS` (nouveau secret) : JSON associant chaque identifiant de routeur à un jeton aléatoire différent d'au moins 32 caractères. Un routeur ne peut appeler que `GET /next` et `POST /result` pour son propre identifiant.
- Sans `ROUTER_TOKENS`, la lecture `/next` renvoie `403 router_not_enrolled`. **Ne pas déployer ce changement sans préparer le secret**, sinon le test précédent ne fonctionnera plus.
- Ne jamais mettre `ADMIN_API_TOKEN` dans le MikroTik ni dans l'APK.

## Préparation du secret (plus tard, uniquement pour test isolé)

Créer un jeton aléatoire distinct par routeur et renseigner un JSON de la forme :
```json
{"router-test-0001":"REMPLACER_PAR_UN_SECRET_ALEATOIRE_DE_32_CARACTERES_MINIMUM"}
```
Le secret JSON se configure avec `npx.cmd wrangler secret put ROUTER_TOKENS` (dans `remote/worker`). Ne jamais le coller dans GitHub ou dans une conversation.

## Limitations bloquantes

Ce changement est un **durcissement de laboratoire**, pas une sécurité commerciale achevée : le jeton admin est encore global, il n'y a pas de connexion client Supabase vérifiée, pas de gestion de propriété/licence, pas de révocation automatisée, pas de limitation de file, pas de délai de bail/anti-rejeu et pas de test matériel. Ne pas connecter le routeur qui dessert des clients.

## Suite

1. Ajouter tests unitaires du Worker et quotas/expiration.
2. Implémenter l'enrôlement sécurisé des routeurs et les autorisations des clients.
3. Tester sur environnement isolé avant toute activation sur le MikroTik de production.
