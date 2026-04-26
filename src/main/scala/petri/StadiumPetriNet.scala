package petri

object StadiumPetriNet:

  val CAPACITE      = 10
  val TOTAL_BILLETS = 10

  // ── Places ──────────────────────────────────────────────────────────
  val pDisponible    = Place("p_dispo",    "Billets disponibles")
  val pVendu         = Place("p_vendu",    "Billets vendus")
  val pScanne        = Place("p_scanne",   "Billets scannés")
  val pExpire        = Place("p_expire",   "Billets expirés")
  val pPersonnes     = Place("p_personnes","Personnes dans le stade")
  val pPlaces        = Place("p_places",   "Places disponibles")
  val pPorteOuverte  = Place("p_porte",    "Porte ouverte")
  val pPeriodeFermee = Place("p_periode",  "Période fermée")

  val places = List(
    pDisponible, pVendu, pScanne, pExpire,
    pPersonnes, pPlaces, pPorteOuverte, pPeriodeFermee
  )

  // ── Transitions ──────────────────────────────────────────────────────
  val tAcheter = Transition("t_acheter", "Acheter billet")
  val tScanner = Transition("t_scanner", "Scanner billet")
  val tQuitter = Transition("t_quitter", "Quitter stade")
  val tExpirer = Transition("t_expirer", "Expirer billet")
  val tOuvrir  = Transition("t_ouvrir",  "Ouvrir portes")
  val tFermer  = Transition("t_fermer",  "Fermer période")

  val transitions = List(tAcheter, tScanner, tQuitter, tExpirer, tOuvrir, tFermer)

  // ── Arcs entrants ────────────────────────────────────────────────────
  // Logique :
  //   p_periode représente maintenant "période FERMÉE"
  //   tOuvrir consomme p_periode (état fermé → ouvre)
  //   tFermer produit p_periode (état ouvert → ferme)
  //   tScanner consomme p_porte et la remet → porte reste ouverte
  val arcsIn = List(
    ArcIn(pDisponible,   tAcheter, 1), // acheter consomme 1 billet dispo
    ArcIn(pVendu,        tScanner, 1), // scanner consomme 1 billet vendu
    ArcIn(pPorteOuverte, tScanner, 1), // scanner nécessite porte ouverte
    ArcIn(pPlaces,       tScanner, 1), // scanner nécessite 1 place dispo
    ArcIn(pPersonnes,    tQuitter, 1), // quitter consomme 1 présence
    ArcIn(pVendu,        tExpirer, 1), // expirer consomme 1 billet vendu
    ArcIn(pPeriodeFermee,tOuvrir,  1), // ouvrir nécessite période fermée
    ArcIn(pPorteOuverte, tFermer,  1), // fermer consomme la porte ouverte
  )

  // ── Arcs sortants ────────────────────────────────────────────────────
  val arcsOut = List(
    ArcOut(tAcheter, pVendu,        1), // acheter produit 1 billet vendu
    ArcOut(tScanner, pScanne,       1), // scanner produit 1 billet scanné
    ArcOut(tScanner, pPorteOuverte, 1), // porte reste ouverte après scan
    ArcOut(tScanner, pPersonnes,    1), // scanner produit 1 présence
    ArcOut(tQuitter, pPlaces,       1), // quitter libère 1 place
    ArcOut(tExpirer, pExpire,       1), // expirer produit 1 billet expiré
    ArcOut(tOuvrir,  pPorteOuverte, 1), // ouvrir produit 1 porte ouverte
    ArcOut(tFermer,  pPeriodeFermee,1), // fermer remet période en état fermé
  )

  val net = PetriNet(places, transitions, arcsIn, arcsOut)

  // ── Marquage initial ─────────────────────────────────────────────────
  // p_periode = 0 au départ car la période EST active (pas fermée)
  // tOuvrir est donc bloqué au départ → les portes s'ouvrent via tFermer d'abord
  //
  // Alternative plus simple : p_periode = 1 signifie "prêt à ouvrir"
  val marquageInitial = Marking(Map(
    pDisponible.id    -> TOTAL_BILLETS,
    pVendu.id         -> 0,
    pScanne.id        -> 0,
    pExpire.id        -> 0,
    pPersonnes.id     -> 0,
    pPlaces.id        -> CAPACITE,
    pPorteOuverte.id  -> 0,
    pPeriodeFermee.id -> 1  // 1 token = prêt à ouvrir
  ))

  // ── Vecteurs d'invariants ────────────────────────────────────────────
  val invariantBillets: Map[String, Int] = Map(
    pDisponible.id -> 1,
    pVendu.id      -> 1,
    pScanne.id     -> 1,
    pExpire.id     -> 1
  )

  val invariantCapacite: Map[String, Int] = Map(
    pPersonnes.id -> 1,
    pPlaces.id    -> 1
  )

  // Invariant porte + période : toujours exactement 1 token entre les deux
  val invariantPortePeriode: Map[String, Int] = Map(
    pPorteOuverte.id  -> 1,
    pPeriodeFermee.id -> 1
  )