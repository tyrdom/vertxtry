package gameplayLib


import gameplayLib.Phrase.Phrase
import gameplayLib.Position.Position
import gameplayLib.Trick.Trick

object Trick extends Enumeration { //如果条件都能找到满足，那么就会触发技能效果，
type Trick = Value


  val AnyCast = Value
  val ThisCast = Value
  val OtherCast = Value

}

case class Skill(phrase: Phrase, position: Position, tricks: Seq[Trick], effects: Seq[CardEffect])

trait CardEffect {

}

case class AvoidDamage(times: Int) extends CardEffect





