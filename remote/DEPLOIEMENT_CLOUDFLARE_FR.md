# Déploiement Cloudflare — MIKROTECK 301 PRO (laboratoire uniquement)

Ce projet est **distinct du portail captif**. Il utilise le **même compte Cloudflare**, mais crée un **Worker séparé** nommé `mikroteck-remote-relay`. Ne modifiez pas le projet Cloudflare Pages du portail captif.

## Ce qui est prêt / ce qui manque
- Prêt dans cette branche : Worker de démonstration, stockage Durable Object, agent RouterOS de lecture seule, tests statiques.
- Manque : authentification individuelle par routeur, autorisation du propriétaire, validation sur un vrai MikroTik, intégration Android, licence commerciale.
- **Ne pas déployer avec les identifiants d'un client. Ne pas importer l'agent sur un routeur de production.**

## 1. Vérifier le compte
1. Ouvrir https://dash.cloudflare.com/ avec le compte qui héberge le portail captif.
2. Ouvrir **Workers & Pages**. Le portail captif doit rester tel quel.
3. Vérifier que les **Workers** et les **Durable Objects (SQLite)** sont disponibles sur votre compte et selon votre offre. Ne pas activer de service payant sans comprendre ses tarifs.

## 2. Préparer un poste de développement
Il faut Node.js récent et Git. Dans un terminal PowerShell sur votre ordinateur :

```powershell
git clone https://github.com/je0515356-spec/mikrotech-301-pro-v2.git
cd mikrotech-301-pro-v2
git checkout feature/remote-control-cloudflare-v1
cd remote/worker
npx wrangler login
```

La commande `wrangler login` ouvre le navigateur pour autoriser votre compte Cloudflare. Elle ne modifie pas votre portail captif.

## 3. Configurer un secret de test
Choisissez un secret aléatoire **uniquement pour le laboratoire**, différent de vos mots de passe et clés Supabase.

```powershell
npx wrangler secret put ADMIN_API_TOKEN
```

Coller le secret dans l'invite sécurisée, **ne pas le mettre dans le dépôt GitHub**. Le prototype utilise encore ce secret partagé : **interdit en production**.

## 4. Déployer le Worker de test
Depuis `remote/worker` :

```powershell
npx wrangler deploy
```

Noter l'adresse `https://mikroteck-remote-relay.<sous-domaine>.workers.dev` indiquée par Cloudflare. L'adresse exacte est fournie par Cloudflare : ne pas la deviner.

Si Cloudflare refuse la migration Durable Object ou demande un changement de forfait, **arrêter ici** et communiquer le message exact avant toute dépense.

## 5. Tester sans MikroTik
Avec PowerShell, remplacer les valeurs suivantes **sur votre ordinateur** :

```powershell
$base = "https://VOTRE-ADRESSE-WORKERS.DEV"
$token = Read-Host "Secret de test ADMIN_API_TOKEN"
$headers = @{ Authorization = "Bearer $token" }
$router = "router-test-0001"

# Doit renvoyer un objet avec job = null
Invoke-RestMethod -Uri "$base/v1/routers/$router/next" -Headers $headers

# Crée une demande de statut, sans action sur le MikroTik
$job = Invoke-RestMethod -Uri "$base/v1/routers/$router/queue" -Method Post -Headers $headers -ContentType "application/json" -Body '{"action":"router_status"}'
$job

# Doit retourner la demande créée
Invoke-RestMethod -Uri "$base/v1/routers/$router/next" -Headers $headers
```

Ne pas partager le secret, ni une capture contenant un jeton. Les résultats de ces tests ne sont **pas encore vérifiés**.

## 6. Test MikroTik ultérieur
Le script `remote/routeros/agent-v7.rsc` est **un prototype à exécuter manuellement sur un routeur de laboratoire uniquement**. Il faut d'abord terminer la sécurisation des identifiants par routeur et valider sa syntaxe RouterOS v7. Ne pas l'installer sur le routeur qui fournit Internet à des clients.

## Prochaine étape technique
Séparer l'authentification administrateur de l'authentification des routeurs, vérifier les droits de propriété, puis ajouter des tests de bout en bout. Après cela seulement, connecter l'APK et effectuer des essais réels.
