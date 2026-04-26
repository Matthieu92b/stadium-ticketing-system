
// src/main/scala/stadium/StadiumActor.scala
package stadium

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import stadium.Protocol._

object StadiumActor {

  case class State(personnes: Int, capacite: Int) {
    def placesDisponibles: Int = capacite - personnes
    def invariantOk: Boolean   = personnes >= 0 && personnes <= capacite
  }

  def apply(capacite: Int): Behavior[StadiumCommand] =
    Behaviors.setup { context =>
      context.log.info("StadiumActor démarré — capacité {}", capacite)
      actif(State(personnes = 0, capacite = capacite))
    }

  private def actif(state: State): Behavior[StadiumCommand] =
    Behaviors.receive { (context, message) =>

      // Invariant surveillé — log d'erreur au lieu de crash
      if (!state.invariantOk) {
        context.log.error(
          "[Stadium] VIOLATION INVARIANT — personnes={}, capacite={}",
          state.personnes, state.capacite
        )
        // On continue en état dégradé plutôt que de crasher
      }

      message match {

        case DemanderEntree(billetId, replyTo) =>
          if (state.placesDisponibles > 0) {
            val newState = state.copy(personnes = state.personnes + 1)
            context.log.info(
              "[Stadium] Entrée {} — {}/{}",
              billetId, newState.personnes, state.capacite
            )
            replyTo ! EntreeAutorisee(billetId, newState.personnes)
            actif(newState)
          } else {
            context.log.warn("[Stadium] Stade plein — refus {}", billetId)
            replyTo ! EntreeRefusee("Stade plein — capacité atteinte")
            Behaviors.same
          }

        case SignalerSortie(replyTo) =>
          if (state.personnes > 0) {
            val newState = state.copy(personnes = state.personnes - 1)
            context.log.info(
              "[Stadium] Sortie — {}/{}", newState.personnes, state.capacite
            )
            replyTo ! SortieEnregistree(newState.personnes)
            actif(newState)
          } else {
            // Sortie impossible si personne présente — on ignore proprement
            context.log.warn("[Stadium] Sortie ignorée — stade déjà vide")
            replyTo ! SortieEnregistree(0)
            Behaviors.same
          }

        case ObtenirEtat(replyTo) =>
          replyTo ! EtatStadium(state.personnes, state.placesDisponibles, state.capacite)
          Behaviors.same
      }
    }
}