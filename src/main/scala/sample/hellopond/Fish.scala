package sample.hellopond

import scala.collection.immutable.{Seq => ISeq}

// Roots of class hierarchies for Events and Commands - inherit your own from them
class Event(val timestamp: Long, val source: String)
class Command

// The Fish interface, in FP style
final case class Fish[S](
  val onEvent: (Event, S) => S,
  val onCommand: (Command, S) => ISeq[Event],
  val initialState: S
)
