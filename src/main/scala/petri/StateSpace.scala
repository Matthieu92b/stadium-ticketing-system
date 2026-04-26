package petri

import scala.collection.mutable

case class StateNode(
                      marking:    Marking,
                      id:         Int,
                      parentId:   Option[Int],
                      transition: Option[Transition]
                    )

case class StateSpace(
                       nodes: List[StateNode],
                       edges: List[(Int, Int, Transition)]
                     ):
  def size: Int = nodes.size

  def deadlocks(net: PetriNet): List[StateNode] =
    nodes.filter(n => net.enabledTransitions(n.marking).isEmpty)

  def markings: List[Marking] = nodes.map(_.marking)

object StateSpaceGenerator:

  def generate(
                net:       PetriNet,
                initial:   Marking,
                maxStates: Int = 10000
              ): StateSpace =
    val visited  = mutable.Map[Marking, Int]()
    val queue    = mutable.Queue[StateNode]()
    val allNodes = mutable.ListBuffer[StateNode]()
    val allEdges = mutable.ListBuffer[(Int, Int, Transition)]()
    var nextId   = 0

    val root = StateNode(initial, nextId, None, None)
    nextId += 1
    visited(initial) = root.id
    queue.enqueue(root)
    allNodes += root

    while queue.nonEmpty && allNodes.size < maxStates do
      val current = queue.dequeue()
      val enabled = net.enabledTransitions(current.marking)

      for t <- enabled do
        net.fire(t, current.marking).foreach { newMarking =>
          val toId = visited.getOrElseUpdate(newMarking, {
            val newNode = StateNode(newMarking, nextId, Some(current.id), Some(t))
            nextId += 1
            queue.enqueue(newNode)
            allNodes += newNode
            newNode.id
          })
          allEdges += ((current.id, toId, t))
        }

    StateSpace(allNodes.toList, allEdges.toList)

  // Distingue vrais deadlocks et états finaux légitimes
  private def estEtatFinalLégitime(node: StateNode): Boolean =
    val m = node.marking
    m.get("p_vendu")     == 0 &&
      m.get("p_personnes") == 0 &&
      m.get("p_periode")   == 0

  def summarize(ss: StateSpace, net: PetriNet): String =
    val sb = new StringBuilder
    sb.append(s"=== Espace d'états ===\n")
    sb.append(s"Marquages accessibles : ${ss.size}\n")
    sb.append(s"Transitions tirées    : ${ss.edges.size}\n")

    val tousDeadlocks  = ss.deadlocks(net)
    val etatsFinaux    = tousDeadlocks.filter(estEtatFinalLégitime)
    val vraisDeadlocks = tousDeadlocks.filterNot(etatsFinaux.contains)

    if vraisDeadlocks.isEmpty then
      sb.append("Deadlocks             : aucun ✓\n")
    else
      sb.append(s"Deadlocks             : ${vraisDeadlocks.size} détecté(s) ✗\n")
      vraisDeadlocks.foreach(n =>
        sb.append(s"  → Marquage #${n.id} : ${n.marking}\n"))

    sb.append(s"États finaux légitimes: ${etatsFinaux.size}\n")
    sb.toString