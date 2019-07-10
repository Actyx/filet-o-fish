package sample.hellopond

import scala.collection.immutable.{Seq => ISeq}

// producer / consumer example encoded as fishes

// producer state
case class ProducerState(val produceCommandEnabled: Boolean, val amountConsumed: Int)

// p on the diagram, we produce a useless Boolean value
case class ProductEvent(override val timestamp: Long, override val source: String)
    extends Event(timestamp, source)

// prod on the diagram
case class ProduceCommand() extends Command

object ProducerFish {
  // when ProductEvent comes, change the state to the value passed in the event
  def onEvent(e: Event, state: ProducerState): ProducerState = {
    e match {
      case ProductEvent(_, _) =>
        return ProducerState(false, state.amountConsumed)
      case ConsumeA(_, _) => return ProducerState(true, state.amountConsumed + 1)
      case ConsumeB(_, _) => return ProducerState(true, state.amountConsumed + 1)
      case other =>
    }
    return state
  }

  // when ProduceCommand comes, pass the value in an event
  def onCommand(c: Command, state: ProducerState): ISeq[Event] = {
    c match {
      case ProduceCommand() => 
      if (state.produceCommandEnabled == true) {
        return Vector(ProductEvent(Timestamp.now(), "producerFish"))
      } else {
        println("producer cannot produce in this state!")
      }
      
      case _ =>
    }
    Vector.empty
  }
  def initialState: ProducerState = ProducerState(true, 0)
}
