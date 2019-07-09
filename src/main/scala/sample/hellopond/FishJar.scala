package sample.hellopond

// this is a type-carrying container for fishes, to store them in a type-variance allowing collection (e.g. Vector[FishJar[_]])
// please note that you need to inform the Jar of the type of the underlying fish, e.g. when your fish is Fish[Int]
// you do it as follows: val intFishJar = new FishJar[Int](intFish)
class FishJar[+S : Manifest](val fish: Fish[_]) {
    def onEvent[U >: S](e: Event, state: U): S = { // but you know, U is really S, :wink: :wink:
        fish match {
            case f: Fish[S] => state match {
                case s: S => return f.onEvent(e, s)
                case _ => throw new IllegalArgumentException("the state passed must be S !") // ain't gonna happen
            }
            case _ => throw new IllegalArgumentException("the contained fish was of wrong type")
        }
    }
    def onCommand[U >: S](c: Command, state: U): Seq[Event] = { // but you know, U is really S, :wink: :wink:
        fish match {
            case f: Fish[S] => state match {
                case s: S => return f.onCommand(c, s)
                case _ => throw new IllegalArgumentException("the state passed must be S !") // ain't gonna happen
            }
            case _ => throw new IllegalArgumentException("the contained fish was of wrong type")
        }
    }
    def toFishState(state: Any): S = { // converter from Any
        state match {
            case s: S => return s
            case _ => throw new IllegalArgumentException("the state passed must be S !")
        }
    }
}