package stadium

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import scala.concurrent.duration.*
import scala.util.{Failure, Success}
import stadium.Protocol.*

object GateActor:

  private given timeout: Timeout = 3.seconds

  def apply(
             billetterie: ActorRef[BilletterieCommand],
             stadium:     ActorRef[StadiumCommand],
             time:        ActorRef[TimeCommand]
           ): Behavior[GateCommand] =
    Behaviors.setup { context =>
      context.log.info("GateActor démarré")
      ouvert(billetterie, stadium, time)
    }

  private def ouvert(
                      billetterie: ActorRef[BilletterieCommand],
                      stadium:     ActorRef[StadiumCommand],
                      time:        ActorRef[TimeCommand]
                    ): Behavior[GateCommand] =
    Behaviors.receive { (context, message) =>
      message match

        case ScannerBillet(billetId, replyTo) =>
          context.log.info("[Gate] Scan demandé — billet {}", billetId)
          context.ask[TimeCommand, TimeResponse](time, ObtenirPeriode.apply) {
            case Success(PeriodeActive)   => ScanInterneEtape2(billetId, replyTo)
            case Success(PeriodeTerminee) => ScanInterneFin(replyTo, PorteFermee("Période terminée"))
            case Failure(_)               => ScanInterneFin(replyTo, PorteFermee("Service temporel indisponible"))
            case Success(_)               => ScanInterneFin(replyTo, PorteFermee("Réponse inattendue TimeActor"))
          }
          Behaviors.same

        case ScanInterneEtape2(billetId, replyTo) =>
          context.ask[BilletterieCommand, BilletterieResponse](billetterie, ValiderBillet(billetId, _)) {
            case Success(BilletValide(id))        => ScanInterneEtape3(id, replyTo)
            case Success(BilletRefuse(_, raison)) => ScanInterneFin(replyTo, PorteFermee(raison))
            case Failure(_)                       => ScanInterneFin(replyTo, PorteFermee("Service billetterie indisponible"))
            case Success(_)                       => ScanInterneFin(replyTo, PorteFermee("Réponse inattendue billetterie"))
          }
          Behaviors.same

        case ScanInterneEtape3(billetId, replyTo) =>
          context.ask[StadiumCommand, StadiumResponse](stadium, DemanderEntree(billetId, _)) {
            case Success(EntreeAutorisee(id, nb)) =>
              context.log.info("[Gate] Entrée accordée — billet {} — {} personnes", id, nb)
              ScanInterneFin(replyTo, PorteOuverte(id))
            case Success(EntreeRefusee(raison)) => ScanInterneFin(replyTo, PorteFermee(raison))
            case Failure(_)                     => ScanInterneFin(replyTo, PorteFermee("Service stade indisponible"))
            case Success(_)                     => ScanInterneFin(replyTo, PorteFermee("Réponse inattendue stade"))
          }
          Behaviors.same

        case ScanInterneFin(replyTo, response) =>
          replyTo ! response
          Behaviors.same

        case OuvrirPorte(replyTo) =>
          replyTo ! PorteEtatOK
          Behaviors.same

        case FermerPorte(replyTo) =>
          context.log.info("[Gate] Fermeture de la porte")
          replyTo ! PorteEtatOK
          ferme(billetterie, stadium, time)
    }

  private def ferme(
                     billetterie: ActorRef[BilletterieCommand],
                     stadium:     ActorRef[StadiumCommand],
                     time:        ActorRef[TimeCommand]
                   ): Behavior[GateCommand] =
    Behaviors.receiveMessage {
      case ScannerBillet(_, replyTo) =>
        replyTo ! PorteFermee("Portes fermées — accès impossible")
        Behaviors.same

      case OuvrirPorte(replyTo) =>
        replyTo ! PorteEtatOK
        ouvert(billetterie, stadium, time)

      case FermerPorte(replyTo) =>
        replyTo ! PorteEtatOK
        Behaviors.same

      case _ => Behaviors.same
    }