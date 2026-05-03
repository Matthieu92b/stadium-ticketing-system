package stadium

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import stadium.Protocol.*
import org.scalatest.matchers.should.Matchers.*
class StadiumActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike:

  "StadiumActor" should {

    "autoriser une entrée si place disponible" in {
      val stadium = spawn(StadiumActor(10))
      val probe   = createTestProbe[StadiumResponse]()

      stadium ! DemanderEntree("B1", probe.ref)
      val rep = probe.expectMessageType[EntreeAutorisee]
      assert(rep.billetId == "B1")
      assert(rep.personnesPresentes == 1)
    }

    "refuser une entrée si stade plein" in {
      val stadium = spawn(StadiumActor(2))
      val probe   = createTestProbe[StadiumResponse]()

      stadium ! DemanderEntree("B1", probe.ref)
      probe.expectMessageType[EntreeAutorisee]

      stadium ! DemanderEntree("B2", probe.ref)
      probe.expectMessageType[EntreeAutorisee]

      // Stade plein — doit refuser
      stadium ! DemanderEntree("B3", probe.ref)
      val refus = probe.expectMessageType[EntreeRefusee]
      assert(refus.raison.contains("plein"))
    }

    "enregistrer une sortie et libérer une place" in {
      val stadium = spawn(StadiumActor(2))
      val probe   = createTestProbe[StadiumResponse]()

      // Remplit le stade
      stadium ! DemanderEntree("B1", probe.ref)
      probe.expectMessageType[EntreeAutorisee]
      stadium ! DemanderEntree("B2", probe.ref)
      probe.expectMessageType[EntreeAutorisee]

      // Une personne sort
      stadium ! SignalerSortie(probe.ref)
      val sortie = probe.expectMessageType[SortieEnregistree]
      assert(sortie.personnesPresentes == 1)

      // Maintenant une entrée doit être autorisée
      stadium ! DemanderEntree("B3", probe.ref)
      probe.expectMessageType[EntreeAutorisee]
    }

    "ignorer une sortie si stade vide" in {
      val stadium = spawn(StadiumActor(10))
      val probe   = createTestProbe[StadiumResponse]()

      stadium ! SignalerSortie(probe.ref)
      val sortie = probe.expectMessageType[SortieEnregistree]
      assert(sortie.personnesPresentes == 0)
    }

    "retourner l'état courant" in {
      val stadium = spawn(StadiumActor(10))
      val probe   = createTestProbe[StadiumResponse]()

      stadium ! DemanderEntree("B1", probe.ref)
      probe.expectMessageType[EntreeAutorisee]

      stadium ! ObtenirEtat(probe.ref)
      val etat = probe.expectMessageType[EtatStadium]
      assert(etat.personnes == 1)
      assert(etat.places    == 9)
      assert(etat.capacite  == 10)
    }

    "respecter l'invariant G(personnes <= capacite) sous charge" in {
      val capacite = 5
      val stadium  = spawn(StadiumActor(capacite))
      val probe    = createTestProbe[StadiumResponse]()

      // Essaie de faire entrer 10 personnes dans un stade de 5
      val resultats = (1 to 10).map { i =>
        stadium ! DemanderEntree(s"B$i", probe.ref)
        probe.expectMessageType[StadiumResponse]
      }

      val accordes = resultats.count(_.isInstanceOf[EntreeAutorisee])
      val refuses  = resultats.count(_.isInstanceOf[EntreeRefusee])

      assert(accordes == capacite, s"Attendu $capacite accordés, obtenu $accordes")
      assert(refuses  == 5,        s"Attendu 5 refusés, obtenu $refuses")
    }
  }