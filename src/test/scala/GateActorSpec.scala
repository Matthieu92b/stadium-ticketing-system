package stadium

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import stadium.Protocol.*

class GateActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike:

  // Helper : crée les 3 acteurs + la gate
  def creerGate(capacite: Int = 10, billets: Int = 10) =
    val time        = spawn(TimeActor())
    val billetterie = spawn(BilletterieActor(billets))
    val stadium     = spawn(StadiumActor(capacite))
    val gate        = spawn(GateActor(billetterie, stadium, time))
    (gate, billetterie, time)

  def demarrerPeriode(time: akka.actor.typed.ActorRef[TimeCommand]): Unit =
    val probe = createTestProbe[TimeResponse]()
    time ! DemarrerPeriode(probe.ref)
    probe.expectMessage(PeriodeActive)

  def acheter(billetId: String, b: akka.actor.typed.ActorRef[BilletterieCommand]): Unit =
    val probe = createTestProbe[BilletterieResponse]()
    b ! AcheterBillet(billetId, probe.ref)
    probe.expectMessage(BilletAchete(billetId))

  "GateActor" should {

    "ouvrir la porte pour un billet valide" in {
      val (gate, bill, time) = creerGate()
      val probe = createTestProbe[GateResponse]()

      demarrerPeriode(time)
      acheter("B1", bill)

      gate ! ScannerBillet("B1", probe.ref)
      val rep = probe.expectMessageType[PorteOuverte]
      assert(rep.billetId == "B1")
    }

    "refuser un billet non acheté" in {
      val (gate, _, time) = creerGate()
      val probe = createTestProbe[GateResponse]()

      demarrerPeriode(time)

      gate ! ScannerBillet("B1", probe.ref)
      val refus = probe.expectMessageType[PorteFermee]
      assert(refus.raison.contains("non acheté"))
    }

    "refuser un billet inconnu" in {
      val (gate, _, time) = creerGate()
      val probe = createTestProbe[GateResponse]()

      demarrerPeriode(time)

      gate ! ScannerBillet("INCONNU", probe.ref)
      probe.expectMessageType[PorteFermee]
    }

    "bloquer la fraude — double scan du même billet" in {
      val (gate, bill, time) = creerGate()
      val probe = createTestProbe[GateResponse]()

      demarrerPeriode(time)
      acheter("B1", bill)

      // Premier scan : doit réussir
      gate ! ScannerBillet("B1", probe.ref)
      probe.expectMessageType[PorteOuverte]

      // Deuxième scan : doit être refusé
      gate ! ScannerBillet("B1", probe.ref)
      val refus = probe.expectMessageType[PorteFermee]
      assert(refus.raison.contains("Fraude"))
    }

    "refuser si stade plein" in {
      val (gate, bill, time) = creerGate(capacite = 1)
      val probe = createTestProbe[GateResponse]()

      demarrerPeriode(time)

      // Remplit le stade (capacité 1)
      acheter("B1", bill)
      gate ! ScannerBillet("B1", probe.ref)
      probe.expectMessageType[PorteOuverte]

      // Deuxième entrée : stade plein
      acheter("B2", bill)
      gate ! ScannerBillet("B2", probe.ref)
      val refus = probe.expectMessageType[PorteFermee]
      assert(refus.raison.contains("plein"))
    }

    "refuser si période terminée" in {
      val (gate, bill, time) = creerGate()
      val probe = createTestProbe[GateResponse]()

      // Période inactive (jamais démarrée)
      acheter("B1", bill)

      gate ! ScannerBillet("B1", probe.ref)
      val refus = probe.expectMessageType[PorteFermee]
      assert(refus.raison.contains("Période") || refus.raison.contains("terminée"))
    }

    "refuser après expiration du billet" in {
      val (gate, bill, time) = creerGate()
      val probe     = createTestProbe[GateResponse]()
      val probeBill = createTestProbe[BilletterieResponse]()

      demarrerPeriode(time)
      acheter("B1", bill)

      // Expire les billets
      bill ! ExpirerBillets(probeBill.ref)
      probeBill.expectMessageType[BilletsExpires]

      gate ! ScannerBillet("B1", probe.ref)
      val refus = probe.expectMessageType[PorteFermee]
      assert(refus.raison.contains("expiré"))
    }

    "gérer plusieurs scans simultanés sans condition de course" in {
      val (gate, bill, time) = creerGate(capacite = 10, billets = 10)
      val probes = (1 to 5).map(_ => createTestProbe[GateResponse]())

      demarrerPeriode(time)
      (1 to 5).foreach(i => acheter(s"B$i", bill))

      // Scans simultanés
      (1 to 5).foreach { i =>
        gate ! ScannerBillet(s"B$i", probes(i-1).ref)
      }

      val resultats = probes.map(_.expectMessageType[GateResponse])
      val accordes  = resultats.count(_.isInstanceOf[PorteOuverte])
      val refuses   = resultats.count(_.isInstanceOf[PorteFermee])

      assert(accordes == 5, s"Attendu 5 accordés, obtenu $accordes")
      assert(refuses  == 0, s"Attendu 0 refusés, obtenu $refuses")
    }
  }