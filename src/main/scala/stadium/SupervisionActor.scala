package stadium

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.duration.*
import stadium.Protocol.*

object SupervisionActor:

  def apply(capacite: Int, totalBillets: Int): Behavior[SupervisionCommand] =
    Behaviors.setup { context =>

      def supervised[T](behavior: Behavior[T]): Behavior[T] =
        Behaviors.supervise(behavior)
          .onFailure[Exception](
            SupervisorStrategy.restart
              .withLimit(maxNrOfRetries = 3, withinTimeRange = 10.seconds)
          )

      val time        = context.spawn(supervised(TimeActor()),                          "time")
      val billetterie = context.spawn(supervised(BilletterieActor(totalBillets)),       "billetterie")
      val stadium     = context.spawn(supervised(StadiumActor(capacite)),               "stadium")
      val gate        = context.spawn(supervised(GateActor(billetterie, stadium, time)), "gate")

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

        case GetBilletterie(replyTo) =>
          replyTo ! billetterie
          Behaviors.same

        case GetStadium(replyTo) =>
          replyTo ! stadium
          Behaviors.same

        case GetTime(replyTo) =>
          replyTo ! time
          Behaviors.same
      }
    }