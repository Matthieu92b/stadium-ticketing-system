package stadium

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern.*
import akka.util.Timeout
import scala.concurrent.duration.*
import scala.concurrent.Await
import stadium.Protocol.*
import stadium.SupervisionActor.*

object Main extends App:

  val system = ActorSystem(
    SupervisionActor(capacite = 500, totalBillets = 500),
    "StadiumSystem"
  )

  // Scala 3 : "given" au lieu de "implicit", types explicites obligatoires
  given timeout: Timeout          = 3.seconds
  given ec: scala.concurrent.ExecutionContext = system.executionContext
  given scheduler: akka.actor.typed.Scheduler = system.scheduler

  // Démarrer la période d'entrée
  system ! Start

  // Récupérer la référence à la porte
  val gateFuture = system.ask[ActorRef[GateCommand]](GetGate.apply)
  val gate       = Await.result(gateFuture, 3.seconds)

  system.log.info("Système opérationnel — gate={}", gate)

  Thread.sleep(30000)
  system.terminate()