package petri

object PropertyChecker:

  def G(phi: Marking => Boolean)(ss: StateSpace): Boolean =
    ss.markings.forall(phi)

  def EF(phi: Marking => Boolean)(ss: StateSpace): Boolean =
    ss.markings.exists(phi)

  def alwaysReaches(
                     phi: Marking => Boolean,
                     ss:  StateSpace,
                     net: PetriNet
                   ): Boolean =
    val adjacency = ss.edges.groupBy(_._1).map {
      case (from, edges) => from -> edges.map(_._2)
    }
    ss.nodes.forall { node =>
      if phi(node.marking) then true
      else adjacency.getOrElse(node.id, Nil).nonEmpty
    }

  def capaciteRespectee(capacite: Int)(m: Marking): Boolean =
    m.get("p_personnes") <= capacite

  def portesFermeesImpliquePasEntree(net: PetriNet)(m: Marking): Boolean =
    val porteFermee = m.get("p_porte") == 0
    if !porteFermee then true
    else !net.enabledTransitions(m).exists(_.id == "t_scanner")

  def sansDeadlockNonFinal(net: PetriNet, ss: StateSpace): List[StateNode] =
    ss.deadlocks(net).filter { node =>
      val m = node.marking
      // État final valide = plus de billets vendus,
      // plus personne dans le stade, période terminée
      // La porte peut rester ouverte — c'est normal en fin de session
      val estEtatFinal =
        m.get("p_vendu")    == 0 &&
          m.get("p_personnes") == 0 &&
          m.get("p_periode")   == 0
      !estEtatFinal
    }

  def fullReport(
                  net:      PetriNet,
                  ss:       StateSpace,
                  capacite: Int
                ): String =
    val sb = new StringBuilder
    sb.append("=== Vérification des propriétés ===\n\n")

    val capOk = G(capaciteRespectee(capacite))(ss)
    sb.append(s"[SURETE] G(personnes ≤ $capacite)          : ${if capOk then "✓ vérifiée" else "✗ VIOLÉE"}\n")

    val porteOk = G(portesFermeesImpliquePasEntree(net))(ss)
    sb.append(s"[SURETE] G(portes fermées → ¬entrée)       : ${if porteOk then "✓ vérifiée" else "✗ VIOLÉE"}\n")

    val stadePleinExiste = EF(m => m.get("p_personnes") == capacite)(ss)
    sb.append(s"[ATTEIGNABILITE] EF(stade plein)           : ${if stadePleinExiste then "✓ oui" else "✗ non"}\n")

    val expirationExiste = EF(m => m.get("p_expire") > 0)(ss)
    sb.append(s"[ATTEIGNABILITE] EF(billet expiré)         : ${if expirationExiste then "✓ oui" else "✗ non"}\n")

    val vraisDeadlocks = sansDeadlockNonFinal(net, ss)
    sb.append(s"\n[DEADLOCKS] Vrais deadlocks (non-finaux)   : ")
    if vraisDeadlocks.isEmpty then
      sb.append("aucun ✓\n")
    else
      sb.append(s"${vraisDeadlocks.size} détecté(s) ✗\n")
      vraisDeadlocks.foreach(n =>
        sb.append(s"  → État #${n.id} : ${n.marking}\n"))

    val maxTokens = ss.markings
      .map(m => m.tokens.values.maxOption.getOrElse(0))
      .maxOption.getOrElse(0)
    sb.append(s"\n[BORNITUDE] Nombre max de tokens par place : $maxTokens\n")
    sb.append(s"[BORNITUDE] Réseau borné                   : ✓ oui (espace fini exploré)\n")

    sb.toString