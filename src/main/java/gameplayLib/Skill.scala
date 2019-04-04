package gameplayLib


import gameplayLib.Trick.Trick

object Trick extends Enumeration {
  type Trick = Value
  val Hold = Value
  val CastWith = Value
  val AnyCast = Value
  val ThisCast = Value
  val OtherCast = Value

}

case class Skill(tricks: Seq[Trick], effects: Seq[CardEffect])

trait CardEffect {

}

case class AvoidDamage(times: Int) extends CardEffect



