# Modélisation et Vérification d'un Système Distribué Critique

> Contrôle d'accès à un stade de football — Akka/Scala 3 + Réseaux de Pétri


## Résultats de vérification formelle

| Propriété | Formule | Résultat |
|---|---|---|
| Pas de surcharge | G(personnes ≤ capacité) | ✓ vérifiée sur 2002 états |
| Portes fermées → pas d'entrée | G(portes fermées → ¬entrée) | ✓ vérifiée |
| Anti-fraude | p_scanne = place puits | ✓ structurel |
| Pas de deadlock | aucun vrai deadlock | ✓ prouvé |
| Réseau borné | max 10 tokens/place | ✓ borné |
| P-invariant billets | p_dispo+p_vendu+p_scanne+p_expire = const. | ✓ structurel + empirique |
| P-invariant capacité | p_personnes+p_places = const. | ✓ structurel + empirique |

---

## Structure du projet

```
stadium-ticketing-system/
├── build.sbt
├── src/
│   ├── main/scala/
│   │   ├── petri/                    # Analyseur réseau de Pétri
│   │   │   ├── PetriNet.scala        # Structure formelle + matrice d'incidence
│   │   │   ├── StadiumPetriNet.scala # Modèle Pétri du stade
│   │   │   ├── StateSpace.scala      # Génération BFS de l'espace d'états
│   │   │   ├── Invariants.scala      # Calcul P-invariants et T-invariants
│   │   │   ├── PropertyChecker.scala # Vérification LTL (G, F, EF)
│   │   │   └── PetriAnalyzer.scala   # Main — rapport complet
│   │   └── stadium/                  # Système Akka distribué
│   │       ├── Protocol.scala        # Messages entre acteurs
│   │       ├── BilletterieActor.scala# Gestion des billets
│   │       ├── StadiumActor.scala    # Gestion de la capacité
│   │       ├── GateActor.scala       # Protocole de scan (5 étapes)
│   │       ├── TimeActor.scala       # Période d'entrée
│   │       ├── SupervisionActor.scala# Supervision + restart strategy
│   │       ├── Main.scala            # Démarrage du système
│   │       └── SimulationConcurrente.scala # 6 scénarios de simulation
│   └── test/scala/
│       ├── BilletterieActorSpec.scala # 9 tests unitaires
│       ├── StadiumActorSpec.scala     # 6 tests unitaires
│       └── GateActorSpec.scala        # 8 tests unitaires
```

---

## Prérequis

- **Java** 11 ou supérieur (testé avec Java 21)
- **sbt** 1.9.7
- **Scala** 3.3.7 (téléchargé automatiquement par sbt)

---

## Installation

```bash
git clone https://github.com/TON_USER/stadium-ticketing-system.git
cd stadium-ticketing-system
sbt compile
```

---

## Lancer le projet

### 1. Analyseur réseau de Pétri

Génère l'espace d'états complet, vérifie les invariants et les propriétés LTL :

```bash
sbt "runMain petri.PetriAnalyzer"
```

Sortie attendue :
```
 Analyseur Réseau de Pétri — Stade   

=== Espace d'états ===
Marquages accessibles : 
Transitions tirées    : 
Deadlocks             : 

P-invariant structurel : 
Vérifié sur tous les états : 

[SURETE] G(personnes ≤ 10) : 
[DEADLOCKS] Vrais deadlocks : 
```

### 2. Système Akka distribué

Démarre les 4 acteurs avec supervision :

```bash
sbt "runMain stadium.Main"
```

### 3. Simulation concurrente (6 scénarios)

```bash
sbt "runMain stadium.SimulationConcurrente"
```

Les 6 scénarios testés :

| Scénario | Description | Propriété vérifiée |
|---|---|---|
| 1 | Entrées séquentielles normales | Flux nominal achat → scan |
| 2 | Fraude — double scan du même billet | Anti-fraude : place puits |
| 3 | Stade plein — refus d'entrée | G(personnes ≤ capacité) |
| 4 | 5 scans simultanés | Sérialisation Akka |
| 5 | Expiration des billets | Transition t_expirer |
| 6 | Sortie et libération de place | Transition t_quitter |

### 4. Tests unitaires

```bash
sbt test
```

20 tests couvrant les 3 acteurs critiques.

---

## Architecture

```
SupervisionActor (restart strategy)
├── BilletterieActor  ←→  p_dispo, p_vendu, p_scanne, p_expire
├── StadiumActor      ←→  p_personnes, p_places
├── GateActor         ←→  protocole de scan 5 étapes (context.ask)
└── TimeActor         ←→  p_periode, p_porte
```

Le protocole de scan suit exactement la transition `t_scanner` du réseau de Pétri :

```
Gate → TimeActor      : période active ?
Gate → Billetterie    : billet vendu ? → marquer scanné
Gate → StadiumActor   : place disponible ? → incrémenter
Gate → Client         : PorteOuverte | PorteFermee(raison)
```

---

## Modèle de Pétri

### Places

| Place | Marquage initial | Sémantique |
|---|---|---|
| p_dispo | 10 | Billets disponibles à la vente |
| p_vendu | 0 | Billets achetés, non scannés |
| p_scanne | 0 | Billets utilisés (historique — place puits) |
| p_expire | 0 | Billets expirés (place puits) |
| p_personnes | 0 | Personnes dans le stade |
| p_places | 10 | Places libres |
| p_porte | 0 | Porte ouverte |
| p_periode | 1 | Prêt à ouvrir |

### P-invariants prouvés

```
P-inv 1 : p_dispo + p_vendu + p_scanne + p_expire = 10  (conservation des billets)
P-inv 2 : p_personnes + p_places = 10                   (conservation de la capacité)
P-inv 3 : p_porte + p_periode = 1                       (mutex porte/période)
```

### Propriétés LTL vérifiées

```
G(personnes ≤ capacité)          — sûreté physique
G(portes fermées → ¬entrée)      — ordre logique respecté
EF(stade plein)                  — atteignabilité
EF(billet expiré)                — expiration possible
G(¬deadlock non-final)           — vivacité
```

---

## Technologies

| Composant | Version |
|---|---|
| Scala | 3.3.7 |
| Akka Typed | 2.6.21 |
| sbt | 1.9.7 |
| ScalaTest | 3.1.4 |
| Java | 21 |

---

## Références

- Petri, C.A. (1962). *Kommunikation mit Automaten*. Thèse, Universität Hamburg
- Pnueli, A. (1977). *The temporal logic of programs*. IEEE FOCS
- Taylor, P. (1990). *The Hillsborough Stadium Disaster — Final Report*. HMSO
- Murata, T. (1989). *Petri Nets: Properties, Analysis and Applications*. Proc. IEEE
- [Akka Typed Documentation](https://akka.io/docs/)
