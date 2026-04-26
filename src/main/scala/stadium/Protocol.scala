

// src/main/scala/stadium/Protocol.scala
package stadium

import akka.actor.typed.ActorRef

object Protocol {

  // ── Messages vers BilletterieActor ──────────────────────────────────
  sealed trait BilletterieCommand
  case class AcheterBillet(billetId: String, replyTo: ActorRef[BilletterieResponse])
    extends BilletterieCommand
  case class ValiderBillet(billetId: String, replyTo: ActorRef[BilletterieResponse])
    extends BilletterieCommand
  case class ExpirerBillets(replyTo: ActorRef[BilletterieResponse])
    extends BilletterieCommand

  sealed trait BilletterieResponse
  case class BilletAchete(billetId: String)          extends BilletterieResponse
  case class BilletValide(billetId: String)          extends BilletterieResponse
  case class BilletRefuse(billetId: String, raison: String) extends BilletterieResponse
  case class BilletsExpires(nb: Int)                 extends BilletterieResponse

  // ── Messages vers StadiumActor ──────────────────────────────────────
  sealed trait StadiumCommand
  case class DemanderEntree(billetId: String, replyTo: ActorRef[StadiumResponse])
    extends StadiumCommand
  case class SignalerSortie(replyTo: ActorRef[StadiumResponse])
    extends StadiumCommand
  case class ObtenirEtat(replyTo: ActorRef[StadiumResponse])
    extends StadiumCommand

  sealed trait StadiumResponse
  case class EntreeAutorisee(billetId: String, personnesPresentes: Int) extends StadiumResponse
  case class EntreeRefusee(raison: String)                              extends StadiumResponse
  case class SortieEnregistree(personnesPresentes: Int)                 extends StadiumResponse
  case class EtatStadium(personnes: Int, places: Int, capacite: Int)    extends StadiumResponse

  // ── Messages vers GateActor ─────────────────────────────────────────
  sealed trait GateCommand
  case class ScannerBillet(billetId: String, replyTo: ActorRef[GateResponse])
    extends GateCommand
  case class OuvrirPorte(replyTo: ActorRef[GateResponse])  extends GateCommand
  case class FermerPorte(replyTo: ActorRef[GateResponse])  extends GateCommand

  case class ScanInterneEtape2(billetId: String, replyTo: ActorRef[GateResponse])
    extends GateCommand

  case class ScanInterneEtape3(billetId: String, replyTo: ActorRef[GateResponse])
    extends GateCommand

  case class ScanInterneFin(replyTo: ActorRef[GateResponse], response: GateResponse)
    extends GateCommand
  
  sealed trait GateResponse
  case class PorteOuverte(billetId: String)   extends GateResponse
  case class PorteFermee(raison: String)      extends GateResponse
  case object PorteEtatOK                     extends GateResponse

  // ── Messages vers TimeActor ─────────────────────────────────────────
  sealed trait TimeCommand
  case class DemarrerPeriode(replyTo: ActorRef[TimeResponse]) extends TimeCommand
  case class TerminerPeriode(replyTo: ActorRef[TimeResponse]) extends TimeCommand
  case class ObtenirPeriode(replyTo: ActorRef[TimeResponse])  extends TimeCommand

  sealed trait TimeResponse
  case object PeriodeActive   extends TimeResponse
  case object PeriodeTerminee extends TimeResponse
}