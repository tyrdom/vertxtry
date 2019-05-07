package gameplayLib


import gameplayLib.Phrase.Phrase
import gameplayLib.Position.Position
import gameplayLib.Who.{All, Opponent, Other, This, Who}


case class PlayerBuffNeed(needBuffEffect: BuffEffect, stack: Int) extends CasterNeedCondition

case class LifeBelow(lifeValue: Int) extends CasterNeedCondition


sealed trait CasterNeedCondition

object CasterNeedCondition {
  def casterConditionIsOk(condition: CasterNeedCondition, activePlayerStatus: OnePlayerStatus): Boolean = condition match {
    case PlayerBuffNeed(needBuffEffect, stack) => activePlayerStatus.buffs.map(x => x.buffEffect).count(y => y.getClass == needBuffEffect.getClass) >= stack
    case LifeBelow(lifeValue) => activePlayerStatus.HP < lifeValue
    case _ => false
  }
}

case class BaseNeedCondition(phrase: Phrase, positions: Seq[Position])


case class CardSkill(baseCondition: BaseNeedCondition, cond: Seq[CasterNeedCondition], effects: Seq[SkillEffect]) {
  def checkAllConditionIsOkThenGetEffect(phrase: Phrase, position: Position, onePlayerStatus: OnePlayerStatus): Seq[SkillEffect] = {
    if (baseCondition.phrase == phrase
      && baseCondition.positions.contains(position)
      && cond.forall(aCasterNeedCond => CasterNeedCondition.casterConditionIsOk(aCasterNeedCond, onePlayerStatus))) {
      effects
    }
    else Nil
  }
}

case class AddBuffToPlayer(toWho: Who, buff: Buff) extends SkillEffect {
  override def activeEffect(caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (Seq[Card], GamePlayGroundValuesThatSkillEffectCanChange) = {
    var oldPlayersStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus
    val newPlayersStatus: Map[String, OnePlayerStatus] = toWho match {
      case This => {
        val status: OnePlayerStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(caster).addBuff(buff)
        oldPlayersStatus += caster -> status
        oldPlayersStatus
      }
      case Other => oldPlayersStatus.map(x => {
        val player = x._1
        val oldStatus = x._2
        val newStatus = if (player == caster) oldStatus else oldStatus.addBuff(buff)
        player -> newStatus
      })
      case Opponent => {
        val status: OnePlayerStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(obj.getOrElse(caster)).addBuff(buff)
        oldPlayersStatus += obj.getOrElse(caster) -> status
        oldPlayersStatus
      }
      case All => oldPlayersStatus.map(x => {
        val player = x._1
        val oldStatus = x._2
        val newStatus = oldStatus.addBuff(buff)
        player -> newStatus
      })
    }
    gamePlayGroundValuesThatSkillEffectCanChange.playersStatus = newPlayersStatus
    (spawningCard, gamePlayGroundValuesThatSkillEffectCanChange)
  }


}

// 暂时用不上
//case class DelBuffFromPlayer(toWho: Who, buff: Buff) extends SkillEffect {
//  override def activeEffect(caster: String, obj: Option[String], spawningCard: Seq[Card],
//                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
//  (Seq[Card], GamePlayGroundValuesThatSkillEffectCanChange) = ???
//}

case class DoDamageToPlayer(toWho: Who, damSeq: Seq[Int]) extends SkillEffect {
  override def activeEffect(caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (Seq[Card], GamePlayGroundValuesThatSkillEffectCanChange) = {
    val oldDamages: Seq[Damage] = gamePlayGroundValuesThatSkillEffectCanChange.nowTurnDamage
    val newDamages = toWho match {
      case This => Seq(Damage(caster, caster, damSeq, false))
      case Other =>
      case Opponent => Seq(Damage(caster, obj.getOrElse(caster), damSeq, false))
      case All =>
    }

    (spawningCard, gamePlayGroundValuesThatSkillEffectCanChange)
  }

}

case class DirectKillPlayer(toWho: Who) extends SkillEffect {
  override def activeEffect(caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (Seq[Card], GamePlayGroundValuesThatSkillEffectCanChange) = ???
}

case class AddBuffToCard(toWho: Who, where: Position, buff: Buff) extends SkillEffect {
  override def activeEffect(caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (Seq[Card], GamePlayGroundValuesThatSkillEffectCanChange) = ???
}

case class DelBuffToCard(toWho: Who, where: Position, buff: Buff) extends SkillEffect {
  override def activeEffect(caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (Seq[Card], GamePlayGroundValuesThatSkillEffectCanChange) = ???
}

case class AddCard(toWho: Who, where: Position, Cards: Seq[Card]) extends SkillEffect {
  override def activeEffect(caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (Seq[Card], GamePlayGroundValuesThatSkillEffectCanChange) = ???
}

case class DropMinCard(toWho: Who, Num: Int) extends SkillEffect {
  override def activeEffect(caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (Seq[Card], GamePlayGroundValuesThatSkillEffectCanChange) = ???
}

case class DropMaxCard(toWho: Who, Num: Int) extends SkillEffect {
  override def activeEffect(caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (Seq[Card], GamePlayGroundValuesThatSkillEffectCanChange) = ???
}

case class DrawCard(toWho: Who, Num: Int) extends SkillEffect {
  override def activeEffect(caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (Seq[Card], GamePlayGroundValuesThatSkillEffectCanChange) = ???
}

case class GiveCard(FromWho: Who, toWho: Who, fromMin: Boolean, Num: Int) extends SkillEffect {
  override def activeEffect(caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (Seq[Card], GamePlayGroundValuesThatSkillEffectCanChange) = ???
}

case class DestroyCertainCard(toWho: Who, wheres: Seq[Position], cardId: Int) extends SkillEffect {
  override def activeEffect(caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (Seq[Card], GamePlayGroundValuesThatSkillEffectCanChange) = ???
}

case class DestroyMaxCard(toWho: Who, where: Seq[Position], Num: Int) extends SkillEffect {
  override def activeEffect(caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (Seq[Card], GamePlayGroundValuesThatSkillEffectCanChange) = ???
}

case object FirstSeatNextRound extends SkillEffect {
  override def activeEffect(caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (Seq[Card], GamePlayGroundValuesThatSkillEffectCanChange) = ???
}

sealed trait SkillEffect {
  def activeEffect(caster: String, obj: Option[String], spawningCard: Seq[Card], gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange): (Seq[Card], GamePlayGroundValuesThatSkillEffectCanChange)
}

object SkillEffect {
  def activeSkillEffectToGamePlayGroundAndSpawnCards(casterToSeqEffect: Map[(String, Option[String]), Seq[SkillEffect]], gamePlayGround: GamePlayGroundValuesThatSkillEffectCanChange, opponent: Option[String], spawningCard: Seq[Card]): (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {
    var GamePlayGroundValuesThatSkillEffectCanChangeToOp = gamePlayGround
    casterToSeqEffect.foreach(x => {
      val caster = x._1._1
      val obj = x._1._2
      val Effects = x._2

    })
    (gamePlayGround, spawningCard)
  }

  def activeSkillEffectToFormCard(casterToSeqEffect: Map[(String, Option[String]), Seq[SkillEffect]], thisShapeNeedCounter: Option[Shape], opShapeNeedCounter: Option[Shape], formCards: Seq[Card]): (Seq[Option[Shape]], Seq[Option[Shape]], Seq[Card]) = ???

}