// src/main/scala/stadium/TimeActor.scala
package stadium

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import stadium.Protocol._

object TimeActor {

  def apply(): Behavior[TimeCommand] =
    Behaviors.setup { context =>
      context.log.info("TimeActor démarré — période inactive")
      inactive()
    }

  private def inactive(): Behavior[TimeCommand] =
    Behaviors.receiveMessage {
      case DemarrerPeriode(replyTo) =>
        replyTo ! PeriodeActive
        active()
      case ObtenirPeriode(replyTo) =>
        replyTo ! PeriodeTerminee
        Behaviors.same
      case TerminerPeriode(replyTo) =>
        replyTo ! PeriodeTerminee
        Behaviors.same
    }

  private def active(): Behavior[TimeCommand] =
    Behaviors.receiveMessage {
      case ObtenirPeriode(replyTo) =>
        replyTo ! PeriodeActive
        Behaviors.same
      case TerminerPeriode(replyTo) =>
        replyTo ! PeriodeTerminee
        inactive()
      case DemarrerPeriode(replyTo) =>
        replyTo ! PeriodeActive
        Behaviors.same
    }
}