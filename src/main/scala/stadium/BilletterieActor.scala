
// src/main/scala/stadium/BilletterieActor.scala
package stadium

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import stadium.Protocol._

object BilletterieActor {

  sealed trait EtatBillet
  case object Disponible extends EtatBillet
  case object Vendu      extends EtatBillet
  case object Scanne     extends EtatBillet
  case object Expire     extends EtatBillet

  case class State(billets: Map[String, EtatBillet], totalBillets: Int) {
    def count(e: EtatBillet): Int = billets.values.count(_ == e)

    // P-invariant : somme constante = totalBillets
    def invariantOk: Boolean =
      count(Disponible) + count(Vendu) + count(Scanne) + count(Expire) == totalBillets

    def stats: String =
      s"dispo=${count(Disponible)} vendu=${count(Vendu)} " +
        s"scanné=${count(Scanne)} expiré=${count(Expire)}"
  }

  def apply(totalBillets: Int): Behavior[BilletterieCommand] =
    Behaviors.setup { context =>
      context.log.info("[Billetterie] Démarré — {} billets", totalBillets)
      val initial = State(
        billets = (1 to totalBillets).map(i => s"B$i" -> (Disponible: EtatBillet)).toMap,
        totalBillets = totalBillets
      )
      actif(initial)
    }

  private def actif(state: State): Behavior[BilletterieCommand] =
    Behaviors.receive { (context, message) =>

      // Vérification P-invariant à chaque message
      if (!state.invariantOk)
        context.log.error("[Billetterie] VIOLATION P-INVARIANT — {}", state.stats)

      message match {

        case AcheterBillet(billetId, replyTo) =>
          state.billets.get(billetId) match {
            case Some(Disponible) =>
              val next = state.copy(billets = state.billets + (billetId -> Vendu))
              context.log.info("[Billetterie] Vendu {} — {}", billetId, next.stats)
              replyTo ! BilletAchete(billetId)
              actif(next)

            case Some(autre) =>
              replyTo ! BilletRefuse(billetId, s"État incompatible : $autre")
              Behaviors.same

            case None =>
              replyTo ! BilletRefuse(billetId, "Billet inconnu")
              Behaviors.same
          }

        case ValiderBillet(billetId, replyTo) =>
          state.billets.get(billetId) match {
            case Some(Vendu) =>
              val next = state.copy(billets = state.billets + (billetId -> Scanne))
              context.log.info("[Billetterie] Scanné {} — {}", billetId, next.stats)
              replyTo ! BilletValide(billetId)
              actif(next)

            case Some(Scanne) =>
              context.log.warn("[Billetterie] FRAUDE détectée — billet {} déjà scanné", billetId)
              replyTo ! BilletRefuse(billetId, "Fraude — billet déjà utilisé")
              Behaviors.same

            case Some(Expire) =>
              replyTo ! BilletRefuse(billetId, "Billet expiré")
              Behaviors.same

            case Some(Disponible) =>
              replyTo ! BilletRefuse(billetId, "Billet non acheté")
              Behaviors.same

            case None =>
              replyTo ! BilletRefuse(billetId, "Billet inconnu")
              Behaviors.same
          }

        case ExpirerBillets(replyTo) =>
          val (aExpirer, reste) = state.billets.partition { case (_, e) => e == Vendu }
          val next = state.copy(
            billets = reste ++ aExpirer.map { case (id, _) => id -> (Expire: EtatBillet) }
          )
          context.log.info("[Billetterie] {} billets expirés — {}", aExpirer.size, next.stats)
          replyTo ! BilletsExpires(aExpirer.size)
          actif(next)
      }
    }
}