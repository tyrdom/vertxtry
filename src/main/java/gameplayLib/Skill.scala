package gameplayLib


import gameplayLib.Phrase.Phrase
import gameplayLib.Position.Position
import gameplayLib.Who.Who
import jdk.nashorn.internal.objects.annotations.Where

object Who extends Enumeration { //如果条件都能找到满足，那么就会触发技能效果，
type Who = Value
  val Any: Who = Value
  val This: Who = Value
  val Other: Who = Value
  val Opponent: Who = Value

}

sealed trait Condition

case class BuffNeed(buff: Buff, stack: Int) extends Condition


case class CardSkill(phrase: Phrase, position: Position, whoTricks: Seq[Who], effects: Seq[SkillEffect])


sealed trait SkillEffect {
}

case class AddBuffToPlayer(toWho: Who, buff: Buff) extends SkillEffect

case class DelBuffFromPlayer(toWho: Who, buff: Buff) extends SkillEffect

case class DoDamageToPlayer(toWho: Who, damSeq: Seq[Int]) extends SkillEffect

case class DirectKillPlayer(toWho: Who) extends SkillEffect

case class AddBuffToCard(toWho: Who, where: Where, buff: Buff) extends SkillEffect

case class DelBuffToCard(toWho: Who, where: Where, buff: Buff) extends SkillEffect

case class AddCard(toWho: Who, where: Where, Cards: Seq[Card]) extends SkillEffect

case class DropMinCard(toWho: Who, Num: Int) extends SkillEffect

case class DropMaxCard(toWho: Who, Num: Int) extends SkillEffect

case class DrawCard(toWho: Who, Num: Int) extends SkillEffect

case class GiveCard(FromWho: Who, toWho: Who, fromMin: Boolean, Num: Int) extends SkillEffect

case class DestroyCertainCard(toWho: Who, where: Where, cardId: Int) extends SkillEffect


object SkillEffect {
  def activeEffect(gameEffect: SkillEffect) = ???
}