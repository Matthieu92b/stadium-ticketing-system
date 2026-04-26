// src/main/scala/stadium/SupervisionActor.scala
package stadium

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.duration._
import stadium.Protocol._

object SupervisionActor {

  sealed trait SupervisionCommand
  case object Start extends SupervisionCommand
  case class GetGate(replyTo: ActorRef[ActorRef[GateCommand]]) extends SupervisionCommand

  def apply(capacite: Int, totalBillets: Int): Behavior[SupervisionCommand] =
    Behaviors.setup { context =>

      // Stratégie : redémarrer l'acteur en cas d'exception, max 3 fois en 10s
      def supervised[T](behavior: Behavior[T]): Behavior[T] =
        Behaviors.supervise(behavior)
          .onFailure[Exception](
            SupervisorStrategy.restart
              .withLimit(maxNrOfRetries = 3, withinTimeRange = 10.seconds)
          )

      val time = context.spawn(
        supervised(TimeActor()),
        "time"
      )

      val billetterie = context.spawn(
        supervised(BilletterieActor(totalBillets)),
        "billetterie"
      )

      val stadium = context.spawn(
        supervised(StadiumActor(capacite)),
        "stadium"
      )

      val gate = context.spawn(
        supervised(GateActor(billetterie, stadium, time)),
        "gate"
      )

      // Surveiller les acteurs enfants
      context.watch(time)
      context.watch(billetterie)
      context.watch(stadium)
      context.watch(gate)

      context.log.info(
        "[Supervision] Système démarré — capacité={} billets={}",
        capacite, totalBillets
      )

      Behaviors.receiveMessage {
        case Start =>
          time ! DemarrerPeriode(context.system.ignoreRef)
          Behaviors.same

        case GetGate(replyTo) =>
          replyTo ! gate
          Behaviors.same
      }
    }
}