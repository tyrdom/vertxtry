package gameplayLib

trait Card {

}

case class CommonCard(originPoint: Int, nowPoint: Int, copy: Boolean) extends Card

case class EffectCard(standard: CommonCard, effect: Effect) extends Card

case class Effect(trickType: TrickType, value: Int)

class TrickType extends Enumeration {
  type TrickType = Value
  val Hold = Value
  val Cast =Value
}

object Card {

}