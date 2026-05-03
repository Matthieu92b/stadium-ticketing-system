package stadium

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.funsuite.AnyFunSuite
import stadium.Protocol.*

class BilletterieActorSpec extends AnyFunSuite:

  val testKit = ActorTestKit()
  import testKit.*

  test("vendre un billet disponible") {
    val billetterie = spawn(BilletterieActor(10))
    val probe       = createTestProbe[BilletterieResponse]()
    billetterie ! AcheterBillet("B1", probe.ref)
    probe.expectMessage(BilletAchete("B1"))
    testKit.stop(billetterie)
  }

  test("refuser un billet deja vendu") {
    val billetterie = spawn(BilletterieActor(10))
    val probe       = createTestProbe[BilletterieResponse]()
    billetterie ! AcheterBillet("B1", probe.ref)
    probe.expectMessage(BilletAchete("B1"))
    billetterie ! AcheterBillet("B1", probe.ref)
    probe.expectMessageType[BilletRefuse]
    testKit.stop(billetterie)
  }

  test("refuser un billet inconnu") {
    val billetterie = spawn(BilletterieActor(10))
    val probe       = createTestProbe[BilletterieResponse]()
    billetterie ! AcheterBillet("INCONNU", probe.ref)
    probe.expectMessageType[BilletRefuse]
    testKit.stop(billetterie)
  }

  test("valider un billet vendu") {
    val billetterie = spawn(BilletterieActor(10))
    val probe       = createTestProbe[BilletterieResponse]()
    billetterie ! AcheterBillet("B1", probe.ref)
    probe.expectMessage(BilletAchete("B1"))
    billetterie ! ValiderBillet("B1", probe.ref)
    probe.expectMessage(BilletValide("B1"))
    testKit.stop(billetterie)
  }

  test("detecter fraude double scan") {
    val billetterie = spawn(BilletterieActor(10))
    val probe       = createTestProbe[BilletterieResponse]()
    billetterie ! AcheterBillet("B1", probe.ref)
    probe.expectMessage(BilletAchete("B1"))
    billetterie ! ValiderBillet("B1", probe.ref)
    probe.expectMessage(BilletValide("B1"))
    billetterie ! ValiderBillet("B1", probe.ref)
    val refus = probe.expectMessageType[BilletRefuse]
    assert(refus.raison.contains("Fraude"))
    testKit.stop(billetterie)
  }

  test("refuser billet non achete") {
    val billetterie = spawn(BilletterieActor(10))
    val probe       = createTestProbe[BilletterieResponse]()
    billetterie ! ValiderBillet("B1", probe.ref)
    probe.expectMessageType[BilletRefuse]
    testKit.stop(billetterie)
  }

  test("expirer les billets vendus") {
    val billetterie = spawn(BilletterieActor(10))
    val probe       = createTestProbe[BilletterieResponse]()
    (1 to 3).foreach { i =>
      billetterie ! AcheterBillet(s"B$i", probe.ref)
      probe.expectMessage(BilletAchete(s"B$i"))
    }
    billetterie ! ExpirerBillets(probe.ref)
    val expire = probe.expectMessageType[BilletsExpires]
    assert(expire.nb == 3)
    testKit.stop(billetterie)
  }

  test("refuser billet expire") {
    val billetterie = spawn(BilletterieActor(10))
    val probe       = createTestProbe[BilletterieResponse]()
    billetterie ! AcheterBillet("B1", probe.ref)
    probe.expectMessage(BilletAchete("B1"))
    billetterie ! ExpirerBillets(probe.ref)
    probe.expectMessageType[BilletsExpires]
    billetterie ! ValiderBillet("B1", probe.ref)
    val refus = probe.expectMessageType[BilletRefuse]
    assert(refus.raison.contains("expir"))
    testKit.stop(billetterie)
  }

  test("P-invariant conservation des billets") {
    val billetterie = spawn(BilletterieActor(5))
    val probe       = createTestProbe[BilletterieResponse]()
    (1 to 2).foreach { i =>
      billetterie ! AcheterBillet(s"B$i", probe.ref)
      probe.expectMessage(BilletAchete(s"B$i"))
      billetterie ! ValiderBillet(s"B$i", probe.ref)
      probe.expectMessage(BilletValide(s"B$i"))
    }
    billetterie ! AcheterBillet("B3", probe.ref)
    probe.expectMessage(BilletAchete("B3"))
    billetterie ! ExpirerBillets(probe.ref)
    probe.expectMessageType[BilletsExpires]
    billetterie ! AcheterBillet("B4", probe.ref)
    probe.expectMessage(BilletAchete("B4"))
    testKit.stop(billetterie)
  }