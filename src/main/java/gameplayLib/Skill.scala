package gameplayLib


import gameplayLib.CardStandType.CardStandType
import gameplayLib.Phrase.Phrase
import gameplayLib.Position.Position
import gameplayLib.Who.{All, Opponent, Other, This, Who}

sealed trait CasterAndCardsNeedCondition {
  def casterAndCardsCondIsOk(cards: Seq[Card], activePlayerStatus: OnePlayerStatus): Boolean
}

case class PlayerBuffNeed(needBuffEffect: BuffEffect, stack: Int) extends CasterAndCardsNeedCondition {
  override def casterAndCardsCondIsOk(cards: Seq[Card], activePlayerStatus: OnePlayerStatus): Boolean = activePlayerStatus.buffs.map(x => x.buffEffect).count(y => y.getClass == needBuffEffect.getClass) >= stack
}

case class LifeBelow(lifeValue: Int) extends CasterAndCardsNeedCondition {
  override def casterAndCardsCondIsOk(cards: Seq[Card], activePlayerStatus: OnePlayerStatus): Boolean = activePlayerStatus.HP < lifeValue
}

case class NotLessThanShape(shape: Shape) extends CasterAndCardsNeedCondition {
  override def casterAndCardsCondIsOk(cards: Seq[Card], activePlayerStatus: OnePlayerStatus): Boolean = {
    val maybeShape: Option[Shape] = Card.genBiggestShape(cards, activePlayerStatus.buffs, 0)
    shape.notBiggerThan(maybeShape)
  }
}


case class BaseNeedCondition(phrase: Phrase, positions: Seq[Position])


case class CardSkill(baseCondition: BaseNeedCondition, cond: Seq[CasterAndCardsNeedCondition], effects: Seq[SkillEffect]) {
  def checkAllConditionIsOkThenGetEffect(phrase: Phrase, position: Position, cards: Seq[Card], onePlayerStatus: OnePlayerStatus): Seq[SkillEffect] = {
    if (baseCondition.phrase == phrase
      && baseCondition.positions.contains(position)
      && cond.forall(aCasterNeedCond => aCasterNeedCond.casterAndCardsCondIsOk(cards, onePlayerStatus))) {
      effects
    }
    else Nil
  }
}


//////////////////////SkillEffectToActive////////////////////
case class AddBuffToPlayer(toWho: Who, buff: Buff) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {
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
    (gamePlayGroundValuesThatSkillEffectCanChange, spawningCard)
  }


}


case class DoDamageToPlayer(toWho: Who, damSeq: Seq[Int]) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {
    val players = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus.keys
    val oldDamages: Seq[Damage] = gamePlayGroundValuesThatSkillEffectCanChange.nowTurnDamage
    val newComeDamages = toWho match {
      case This => Seq(Damage(caster, caster, damSeq, false))
      case Other => players.filter(x => x != caster).map(obj => Damage(caster, obj, damSeq, false)).toSeq
      case Opponent => Seq(Damage(caster, obj.getOrElse(caster), damSeq, false))
      case All => players.map(obj => Damage(caster, obj, damSeq, false)).toSeq
    }
    val newDamage = oldDamages ++ newComeDamages
    gamePlayGroundValuesThatSkillEffectCanChange.nowTurnDamage = newDamage
    (gamePlayGroundValuesThatSkillEffectCanChange, spawningCard)
  }
}

case class DirectKillPlayer(toWho: Who) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {

    val killPlayers = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj)

    val oldStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus
    killPlayers.foreach(aPlayer => {
      val newOneStatus = oldStatus(aPlayer).kill
      gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += (aPlayer -> newOneStatus)
    })
    (gamePlayGroundValuesThatSkillEffectCanChange, spawningCard)
  }
}

case class AddBuffToCard(toWho: Who, wheres: Seq[Position], buff: Buff) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {

    val addPlayers = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj)
    var newSpawningCard = spawningCard
    wheres.foreach(x => {
      val aWhere = x
      aWhere match {
        case gameplayLib.Position.DrawDeck => gamePlayGroundValuesThatSkillEffectCanChange.drawDeck = gamePlayGroundValuesThatSkillEffectCanChange.drawDeck.map(_.addBuff(buff))
        case gameplayLib.Position.DropDeck => gamePlayGroundValuesThatSkillEffectCanChange.dropDeck = gamePlayGroundValuesThatSkillEffectCanChange.dropDeck.map(_.addBuff(buff))
        case theWhere if theWhere == gameplayLib.Position.HandCards || theWhere == gameplayLib.Position.MyHandCards || theWhere == gameplayLib.Position.OtherHandCards || theWhere == gameplayLib.Position.OpponentHandCards => {
          addPlayers.foreach(who => {
            val newOneStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(who).addHandCardBuff(buff)
            gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += who -> newOneStatus
          })
        }
        case theWhere if theWhere == gameplayLib.Position.SpawnCard || theWhere == gameplayLib.Position.MySpawnCards || theWhere == gameplayLib.Position.OtherSpawnCards || theWhere == gameplayLib.Position.OpponentSpawnCards =>
          newSpawningCard = spawningCard.map(_.addBuff(buff))
      }
    })
    (gamePlayGroundValuesThatSkillEffectCanChange, newSpawningCard)
  }
}

case class DelBuffFromPlayer(toWho: Who, buffEffect: BuffEffect) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {
    val delPlayer = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj)
    delPlayer.foreach(who => {
      val newOneStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(who).delBuffFromPlayer(buffEffect)
      gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += who -> newOneStatus
    })

    (gamePlayGroundValuesThatSkillEffectCanChange, spawningCard)
  }

}

case class DelBuffFromCard(toWho: Who, wheres: Seq[Position], buffEffect: BuffEffect) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {
    val delPlayers = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj)

    var newSpawningCard = spawningCard
    wheres.foreach(x => {
      val aWhere = x
      aWhere match {
        case gameplayLib.Position.DrawDeck => gamePlayGroundValuesThatSkillEffectCanChange.drawDeck = gamePlayGroundValuesThatSkillEffectCanChange.drawDeck.map(_.delBuff(buffEffect))
        case gameplayLib.Position.DropDeck => gamePlayGroundValuesThatSkillEffectCanChange.dropDeck = gamePlayGroundValuesThatSkillEffectCanChange.dropDeck.map(_.delBuff(buffEffect))
        case theWhere if theWhere == gameplayLib.Position.HandCards || theWhere == gameplayLib.Position.MyHandCards || theWhere == gameplayLib.Position.OtherHandCards || theWhere == gameplayLib.Position.OpponentHandCards => {
          delPlayers.foreach(who => {
            val newOneStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(who).delHandCardBuff(buffEffect)
            gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += who -> newOneStatus
          })
        }
        case theWhere if theWhere == gameplayLib.Position.SpawnCard || theWhere == gameplayLib.Position.MySpawnCards || theWhere == gameplayLib.Position.OtherSpawnCards || theWhere == gameplayLib.Position.OpponentSpawnCards =>
          newSpawningCard = spawningCard.map(_.delBuff(buffEffect))
      }
    })
    (gamePlayGroundValuesThatSkillEffectCanChange, newSpawningCard)
  }
}


case class AddExtraCard(toWho: Who, wheres: Seq[Position], cardIds: Seq[Int], cardStandType: CardStandType) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {

    val addCardPlayers = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj)
    var newSpawningCard = spawningCard
    val oldGenId = gamePlayGroundValuesThatSkillEffectCanChange.genIdForCard
    val newGenId = oldGenId + cardIds.length
    val tuples: Seq[(Int, Int)] = cardIds.zip(oldGenId until newGenId)
    val cardsToAdd = tuples.map(x => Card(x._1, x._2, 3, 4, cardStandType, None, Nil, Nil))

    gamePlayGroundValuesThatSkillEffectCanChange.genIdForCard = newGenId

    wheres.foreach(x => {
      val aWhere = x
      aWhere match {
        case gameplayLib.Position.DrawDeck => gamePlayGroundValuesThatSkillEffectCanChange.drawDeck = gamePlayGroundValuesThatSkillEffectCanChange.drawDeck ++ cardsToAdd
        case gameplayLib.Position.DropDeck => gamePlayGroundValuesThatSkillEffectCanChange.dropDeck = gamePlayGroundValuesThatSkillEffectCanChange.dropDeck ++ cardsToAdd
        case theWhere if theWhere == gameplayLib.Position.HandCards || theWhere == gameplayLib.Position.MyHandCards || theWhere == gameplayLib.Position.OtherHandCards || theWhere == gameplayLib.Position.OpponentHandCards => {
          addCardPlayers.foreach(who => {
            val newOneStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(who).addHandCard(cardsToAdd)
            gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += who -> newOneStatus
          })
        }
        case theWhere if theWhere == gameplayLib.Position.SpawnCard || theWhere == gameplayLib.Position.MySpawnCards || theWhere == gameplayLib.Position.OtherSpawnCards || theWhere == gameplayLib.Position.OpponentSpawnCards =>
          newSpawningCard = spawningCard ++ cardsToAdd
      }
    })
    (gamePlayGroundValuesThatSkillEffectCanChange, newSpawningCard)
  }
}

case class MoveCard(cardId: Int, fromWheres: Seq[Position], fromWho: Who, toWhere: Position, toWho: Who, maxNum: Int) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = ???
}

case class DropMinCard(toWho: Who, Num: Int) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = ???
}

case class DropMaxCard(toWho: Who, Num: Int) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = ???
}

case class DrawCard(toWho: Who, Num: Int) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = ???
}

case class GiveCard(FromWho: Who, toWho: Who, fromMin: Boolean, Num: Int) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = ???
}

case class DestroyCertainCard(toWho: Who, wheres: Seq[Position], cardId: Int) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = ???
}

case class DestroyMaxCard(toWho: Who, where: Seq[Position], Num: Int) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = ???
}

case object FirstSeatNextRound extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = ???
}

sealed trait SkillEffect {
  def activeEffect(gid: Int, caster: String, obj: Option[String], spawningCard: Seq[Card], gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange): (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card])
}

object SkillEffect {
  def activeSkillEffectToGamePlayGroundAndSpawnCards(casterToSeqEffect: Map[(String, Option[String], Int), Seq[SkillEffect]], gamePlayGround: GamePlayGroundValuesThatSkillEffectCanChange, opponent: Option[String], spawningCard: Seq[Card]): (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {

    casterToSeqEffect.foldLeft((gamePlayGround, spawningCard))((sAndG, x) => {
      val theSpawnCard = sAndG._2
      val theGamePlay = sAndG._1
      val caster = x._1._1
      val obj = x._1._2
      val genId = x._1._3
      val Effects: Seq[SkillEffect] = x._2
      Effects.foldLeft((theGamePlay, theSpawnCard))((tuple, effect) => {
        val s = tuple._2
        val g = tuple._1
        effect.activeEffect(genId, caster, obj, s, g)
      })
    })
  }

  def getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange, toWho: Who, caster: String, obj: Option[String]): Iterable[String] = {
    val players = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus.keys
    toWho match {
      case This => Seq(caster)
      case Other => players.filter(x => x != caster)
      case Opponent => Seq(obj.getOrElse(caster))
      case All => players
    }
  }

  def activeSkillEffectToFormCard(casterToSeqEffect: Map[(String, Option[String], Int), Seq[SkillEffect]], thisShapeNeedCounter: Option[Shape], opShapeNeedCounter: Option[Shape], formCards: Seq[Card]): (Seq[Option[Shape]], Seq[Option[Shape]], Seq[Card]) = ???

}

sealed trait FormCardSkillEffect {

  case class FormSkillResult(thisShapeCounterAfterSkill: Option[Shape], opShapeCounterAfterSkill: Option[Shape], formCardsAfterSkill: Seq[Card])

  def activeSkillEffectToFormCard(casterToSeqEffect: Map[(String, Option[String], Int), Seq[SkillEffect]], gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange, thisShapeNeedCounter: Option[Shape], opShapeNeedCounter: Option[Shape], formCards: Seq[Card]):
  FormSkillResult = ???
}