package sample.hellopond

import scala.collection.immutable.{Seq => ISeq}

// producer / consumer example encoded as fishes

// consumer state
case class ConsumerState(val consumeCommandEnabled: Boolean)

// c1 / c2 on the diagram
case class ConsumeA(override val timestamp: Int, override val source: String)
    extends Event(timestamp, source)

case class ConsumeB(override val timestamp: Int, override val source: String)
    extends Event(timestamp, source)

// cons on the diagram
case class ConsumeCommand() extends Command

object ConsumerA {
  // when ProductEvent comes, enable the ConsumeCommand, by setting the state to true
  def onEvent(e: Event, state: ConsumerState): ConsumerState = {
    e match {
      case ProductEvent(_, _) =>
        return ConsumerState(true)
      case ConsumeA(_, _) => return ConsumerState(false)
      case ConsumeB(_, _) => return ConsumerState(false)
      case other =>
    }
    return state
  }

  // when ProduceCommand comes, pass the value in an event
  def onCommand(c: Command, state: ConsumerState): ISeq[Event] = {
    c match {
      case ConsumeCommand() =>
        if (state.consumeCommandEnabled == true) {
          return Vector(ConsumeA(Timestamp.now(), "consumerA"))
        } else {
            println("consumerA cannot consume in this state!")
        }
      case _ =>
    }
    Vector.empty
  }
  def initialState: ConsumerState = ConsumerState(false)
}

object ConsumerB {
  // when ProductEvent comes, enable the ConsumeCommand, by setting the state to true
  def onEvent(e: Event, state: ConsumerState): ConsumerState = {
    e match {
      case ProductEvent(_, _) =>
        return ConsumerState(true)
      case ConsumeA(_, _) => return ConsumerState(false)
      case ConsumeB(_, _) => return ConsumerState(false)
      case other =>
    }
    return state
  }

  // when ProduceCommand comes, pass the value in an event
  def onCommand(c: Command, state: ConsumerState): ISeq[Event] = {
    c match {
      case ConsumeCommand() =>
        if (state.consumeCommandEnabled == true) {
          return Vector(ConsumeB(Timestamp.now(), "consumerB"))
        } else {
          println("consumerB cannot consume in this state!")
        }
      case _ =>
    }
    Vector.empty
  }
  def initialState: ConsumerState = ConsumerState(false)
}
