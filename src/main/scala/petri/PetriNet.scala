// src/main/scala/petri/PetriNet.scala
package petri

// ── Éléments du réseau ────────────────────────────────────────────────

case class Place(id: String, name: String)

case class Transition(id: String, name: String)

// Arc entrant : place → transition (consomme `weight` tokens)
case class ArcIn(place: Place, transition: Transition, weight: Int = 1)

// Arc sortant : transition → place (produit `weight` tokens)
case class ArcOut(transition: Transition, place: Place, weight: Int = 1)

// Marquage = état du réseau : place → nombre de tokens
case class Marking(tokens: Map[String, Int]) {
  def get(placeId: String): Int = tokens.getOrElse(placeId, 0)

  def apply(placeId: String): Int = get(placeId)

  override def toString: String =
    tokens.filter(_._2 > 0)
      .map { case (id, n) => s"$id:$n" }
      .mkString("{", ", ", "}")
}

// ── Le réseau de Pétri ─────────────────────────────────────────────────

case class PetriNet(
                     places:      List[Place],
                     transitions: List[Transition],
                     arcsIn:      List[ArcIn],
                     arcsOut:     List[ArcOut]
                   ) {

  // Précondition d'une transition : places consommées et quantités
  def precond(t: Transition): Map[String, Int] =
    arcsIn.collect {
      case ArcIn(p, tr, w) if tr == t => p.id -> w
    }.toMap

  // Postcondition d'une transition : places produites et quantités
  def postcond(t: Transition): Map[String, Int] =
    arcsOut.collect {
      case ArcOut(tr, p, w) if tr == t => p.id -> w
    }.toMap

  // Une transition est franchissable si toutes ses préconditions sont satisfaites
  def enabled(t: Transition, m: Marking): Boolean =
    precond(t).forall { case (placeId, weight) => m.get(placeId) >= weight }

  // Tirer une transition : produit le nouveau marquage
  def fire(t: Transition, m: Marking): Option[Marking] =
    if (!enabled(t, m)) None
    else {
      val consumed = precond(t).foldLeft(m.tokens) {
        case (tokens, (pid, w)) => tokens + (pid -> (tokens.getOrElse(pid, 0) - w))
      }
      val produced = postcond(t).foldLeft(consumed) {
        case (tokens, (pid, w)) => tokens + (pid -> (tokens.getOrElse(pid, 0) + w))
      }
      Some(Marking(produced))
    }

  // Toutes les transitions franchissables depuis un marquage
  def enabledTransitions(m: Marking): List[Transition] =
    transitions.filter(enabled(_, m))

  // Matrice d'incidence : colonne par transition, ligne par place
  // incidence(place)(transition) = postcond - precond
  def incidenceMatrix: Map[String, Map[String, Int]] =
    places.map { p =>
      p.id -> transitions.map { t =>
        val post = postcond(t).getOrElse(p.id, 0)
        val pre  = precond(t).getOrElse(p.id, 0)
        t.id -> (post - pre)
      }.toMap
    }.toMap
}