package sample.hellopond

import scala.collection.immutable.{Seq => ISeq}

case class CounterEvent(override val timestamp: Int, override val source: String, value: Int) extends Event(timestamp, source)

case class CounterAddCommand(value: Int) extends Command

class CounterFish extends Fish[Int] {
    def onEvent(e: Event, state:Int): Int = {
        e match {
            case CounterEvent(_, _, value) =>
                println(s"counterFish, will return state of ${state+value}")
                return state + value
            case other => println(s"unrecognized message: $other")
        }
        return state
    }
    def onCommand(c: Command, state: Int): ISeq[Event] = {
        c match {
            case CounterAddCommand(value) => return Vector(CounterEvent(Timestamp.now(), "counterFish", value))
            case _ =>
        }
        Vector.empty
    }
    def initialState: Int = 0
}
