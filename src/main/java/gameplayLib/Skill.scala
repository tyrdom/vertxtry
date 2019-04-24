package gameplayLib


import gameplayLib.Phrase.Phrase
import gameplayLib.Position.Position
import gameplayLib.Who.Who

object Who extends Enumeration { //如果条件都能找到满足，那么就会触发技能效果，
type Who = Value
  val Any: Who = Value
  val This: Who = Value
  val Other: Who = Value
  val Opponent: Who = Value

}



case class CardSkill(phrase: Phrase, position: Position, whoTricks: Seq[Who], effects: Seq[GameEffectToAct])



trait GameEffectToAct {
 def activeEffect
}

case class AvoidDamage(times: Int,round:Int) extends GameEffectToAct
{
  def activeEffect = ???
}




trait  EffectResult{ // 作用发动会产生结果

}