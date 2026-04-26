// src/main/scala/petri/Invariants.scala
package petri

object Invariants {

  /**
   * Vérifie qu'un vecteur `inv` est un P-invariant du réseau.
   * Condition : pour chaque transition t, sum(inv(p) * C(p,t)) = 0
   * où C est la matrice d'incidence.
   *
   * Autrement dit : inv · C = 0
   */
  def isPInvariant(net: PetriNet, inv: Map[String, Int]): Boolean = {
    val matrix = net.incidenceMatrix
    net.transitions.forall { t =>
      val sum = inv.map { case (pid, coeff) =>
        val delta = matrix.get(pid).flatMap(_.get(t.id)).getOrElse(0)
        coeff * delta
      }.sum
      sum == 0
    }
  }

  /**
   * Vérifie qu'un vecteur `inv` est un T-invariant du réseau.
   * Condition : pour chaque place p, sum(inv(t) * C(p,t)) = 0
   * Autrement dit : C · inv = 0
   * Un T-invariant correspond à une séquence de tirs qui ramène
   * au marquage initial (cycle).
   */
  def isTInvariant(net: PetriNet, inv: Map[String, Int]): Boolean = {
    val matrix = net.incidenceMatrix
    net.places.forall { p =>
      val row = matrix.getOrElse(p.id, Map.empty)
      val sum = inv.map { case (tid, coeff) =>
        val delta = row.getOrElse(tid, 0)
        coeff * delta
      }.sum
      sum == 0
    }
  }

  /**
   * Calcule la valeur d'un P-invariant sur un marquage donné.
   * Si c'est un P-invariant valide, cette valeur est constante
   * pour TOUS les marquages accessibles.
   */
  def invariantValue(inv: Map[String, Int], marking: Marking): Int =
    inv.map { case (pid, coeff) => coeff * marking.get(pid) }.sum

  /**
   * Vérifie qu'un P-invariant est conservé sur TOUS les marquages
   * de l'espace d'états (vérification empirique exhaustive).
   */
  def checkInvariantAllStates(
                               inv:        Map[String, Int],
                               stateSpace: StateSpace,
                               expected:   Int
                             ): Boolean =
    stateSpace.markings.forall(m => invariantValue(inv, m) == expected)

  /** Rapport complet des invariants */
  def report(
              net:        PetriNet,
              stateSpace: StateSpace,
              invariants: List[(String, Map[String, Int], Int)]
            ): String = {
    val sb = new StringBuilder
    sb.append("=== Vérification des invariants ===\n")

    invariants.foreach { case (name, inv, expected) =>
      val isStructural = isPInvariant(net, inv)
      val isEmprical   = checkInvariantAllStates(inv, stateSpace, expected)

      sb.append(s"\nInvariant : $name\n")
      sb.append(s"  Vecteur  : ${inv.map { case (k,v) => s"$k×$v" }.mkString(" + ")}\n")
      sb.append(s"  Valeur attendue : $expected\n")
      sb.append(s"  P-invariant structurel : ${if (isStructural) "✓ oui" else "✗ non"}\n")
      sb.append(s"  Vérifié sur tous les états : ${if (isEmprical) "✓ oui" else "✗ non"}\n")
    }
    sb.toString
  }
}