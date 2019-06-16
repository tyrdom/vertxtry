package gameplayLib

import gameplayLib.CardStandType.CardStandType
import gameplayLib.Phrase.Phrase
import gameplayLib.Position.Position

import scala.util.Random


case class Shape(keyPoint: Int, height: Int, length: Int, extraNum: Int, fillBlankRestNum: Int) {
  def notBiggerThan(maybeShape: Option[Shape]): Boolean = maybeShape match {
    case None => false
    case Some(aShape) =>
      aShape.keyPoint >= this.keyPoint && aShape.height >= this.height && aShape.length >= this.length && aShape.extraNum == this.extraNum && aShape.fillBlankRestNum == 0
  }


}

object CardStandType extends Enumeration { //位置分类
type CardStandType = Value
  val Original: CardStandType = Value
  val TempCopy: CardStandType = Value

}

//卡牌的一般属性 id：牌的配置id ，大于10点可以当作任意点数，小于1点只能当作单独牌出，copy为此牌是否为复制牌 ,genId为本次比赛此卡唯一Id
case class Card(id: Int, genId: Int, level: Int, Point: Int, standType: CardStandType, ownerCharacterId: Option[Int], skills: Seq[CardSkill], var buffs: Seq[Buff] = Nil) {
  def addBuff(buff: Buff): Card = {
    this.buffs = buff +: this.buffs
    this
  }

  def delBuff(buffEffect: BuffEffect): Card = {
    this.buffs = this.buffs.filter(buff => buff.buffEffect != buffEffect)
    this
  }
}


object Card {

  def activeCardSkillWhenSpawn(oldSpawningCard: Seq[Card], caster: String, obj: String, oldGamePlayGround: GamePlayGroundValuesThatSkillEffectCanChange): (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {
    val phrase = Phrase.Spawn
    val charId2OwnPlayerAndLevel = genMapCharIdToOwnPlayerAndLevel(oldGamePlayGround)
    val tupleToEffects = getEffectsFromCards(oldGamePlayGround, phrase, Position.SpawnCard, oldSpawningCard, Some(caster), Some(obj), charId2OwnPlayerAndLevel)
    SkillEffect.activeSkillEffectToGamePlayGroundAndSpawnCards(tupleToEffects, oldGamePlayGround, Some(obj), oldSpawningCard)
  }

  def activeCardSkillWhenForm(thisNeedCounterShape: Option[Shape], obNeedCounterShape: Option[Shape], formCards: Seq[Card], caster: String, obj: String, nowGamePlayGround: GamePlayGroundValuesThatSkillEffectCanChange): (Seq[Option[Shape]], Seq[Option[Shape]], Seq[Card]) = { //form阶段不可改变游戏状态，只能改变将要Form的牌
    val phrase = Phrase.FormCards
    val charId2OwnPlayerAndLevel = genMapCharIdToOwnPlayerAndLevel(nowGamePlayGround)
    val stringToEffects = getEffectsFromCards(nowGamePlayGround, phrase, Position.MyHandCards, formCards, None, None, charId2OwnPlayerAndLevel)
    SkillEffect.activeSkillEffectToFormCard(stringToEffects, thisNeedCounterShape, obNeedCounterShape, formCards)
  }

  def activeCardSkillWhenCheck(oldGamePlayGround: GamePlayGroundValuesThatSkillEffectCanChange): GamePlayGroundValuesThatSkillEffectCanChange = { //在check流程，发动牌组堆，手牌，弃牌堆的卡牌技能
    val phrase = Phrase.Check
    val charId2OwnPlayerAndLevel = genMapCharIdToOwnPlayerAndLevel(oldGamePlayGround)


    val drawDeck = oldGamePlayGround.drawDeck
    val posD = Position.DrawDeck
    val tupleToEffects: Map[(String, Option[String],Int), Seq[SkillEffect]] = getEffectsFromCards(oldGamePlayGround, phrase, posD, drawDeck, None, None, charId2OwnPlayerAndLevel)
    val stringToEffects1 = tupleToEffects

    val playGround1 = SkillEffect.activeSkillEffectToGamePlayGroundAndSpawnCards(stringToEffects1, oldGamePlayGround, None, Nil)

    val dropDeck = oldGamePlayGround.drawDeck
    val posP = Position.DropDeck
    val stringToEffects2 = getEffectsFromCards(oldGamePlayGround, phrase, posP, dropDeck, None, None, charId2OwnPlayerAndLevel)

    val playGround2 = SkillEffect.activeSkillEffectToGamePlayGroundAndSpawnCards(stringToEffects2, playGround1._1, None, Nil)

    val stringToEffectsMaps3 = oldGamePlayGround.playersStatus.map(x => {
      val holder = x._1
      val handCards = x._2.handCards
      val pos = Position.HandCards
      getEffectsFromCards(oldGamePlayGround, phrase, pos, handCards, Some(holder), None, charId2OwnPlayerAndLevel)
    })
    val playGround3 = stringToEffectsMaps3.foldLeft(playGround2)((playG, aMap) => SkillEffect.activeSkillEffectToGamePlayGroundAndSpawnCards(aMap, playG._1, None, Nil))
    playGround3._1
  }


  def genMapCharIdToOwnPlayerAndLevel(playGround: GamePlayGroundValuesThatSkillEffectCanChange): Map[Int, Seq[(String, Int)]] = { //通过当期状态生成角色相关的玩家映射表，支持多个玩家拥有同样id的角色，等级不同
    val ownerAndCharLvTuple = playGround.playersStatus.flatMap(
      x => {
        val owner = x._1
        val chars = x._2.characters
        chars.map(x => (x.id, owner, x.level))
      }
    )
    var charIdToOwnPlayerAndLevel = Map(): Map[Int, Seq[(String, Int)]]
    ownerAndCharLvTuple.foreach(x => {
      val oldSeq = charIdToOwnPlayerAndLevel.getOrElse(x._1, Nil)
      charIdToOwnPlayerAndLevel += (x._1 -> ((x._2, x._3) +: oldSeq))
    })
    charIdToOwnPlayerAndLevel
  }


  //通过映射表和卡来生成发动技能信息 谁对谁释放了什么技能
  def getEffectsFromCards(gamePlayGround: GamePlayGroundValuesThatSkillEffectCanChange, phrase: Phrase, position: Position, cards: Seq[Card], cardsOwner: Option[String], cardsObj: Option[String], charIdToOwnPlayerAndLevel: Map[Int, Seq[(String, Int)]]): Map[(String, Option[String],Int), Seq[SkillEffect]] = {
    val playersStatus = gamePlayGround.playersStatus
    val cardsCanEffectGroupByCharId: Map[Int, Seq[Card]] = cards.groupBy(_.ownerCharacterId.getOrElse(-1)) //所有玩家角色按照角色ID分组,没有定义的设为-1
    var caster2EffectMap = Map(): Map[(String, Option[String], Int), Seq[SkillEffect]] //玩家对玩家，通过哪张牌genId标识 - 释放效果队列
    cardsCanEffectGroupByCharId.keys.foreach(charId => { //对于
      val cards = cardsCanEffectGroupByCharId(charId)
      val tuples = charIdToOwnPlayerAndLevel.getOrElse(charId, Nil)
      tuples.foreach(aTuple => {
        val caster = aTuple._1
        val level = aTuple._2

        val newPos = position match {
          case Position.HandCards => if (caster == cardsOwner.get) Position.MyHandCards else Position.OtherHandCards //手牌方位判断，如果
          case Position.SpawnCard => if (caster == cardsOwner.get) Position.MySpawnCards else if (caster == cardsObj.get) Position.OpponentSpawnCards else Position.OtherSpawnCards
          case _ => position
        }
        cards.foreach(card => if (card.level <= level) {
          val skills = card.skills.flatMap(_.checkAllConditionIsOkThenGetEffect(phrase, newPos, cards, playersStatus(caster)))

          val gId = card.genId
          caster2EffectMap += (caster, cardsObj, gId) -> skills
        } else Nil)


      })
    })
    caster2EffectMap
  }

  def sortHandCard(Cards: Seq[Card], characters: Seq[Character]): Seq[Card] = {
    val charactersExpMap = characters.map(x => x.id -> x.exp).toMap

    def getCharacterExp(card: Card) =
      if (card.ownerCharacterId.isEmpty) {
        0
      }
      else {
        val exp = charactersExpMap.getOrElse(card.ownerCharacterId.get, 0)
        exp
      }

    def compareCardLessThan(a: Card, b: Card): Boolean = a.Point < b.Point || (a.Point == b.Point && getCharacterExp(a) < getCharacterExp(b)) || (a.Point == b.Point && getCharacterExp(a) == getCharacterExp(b) && a.genId < b.genId) //genId为一局游戏中生成的id
    Cards.sortWith(compareCardLessThan) //  按点数排列卡牌，从小到大排列，某些技能用到此功能
  }

  def shuffleCard(Cards: Seq[Card]): Seq[Card] = Random.shuffle(Cards)

  def genPointMapAndSpecial(Cards: Seq[Card], buffs: Seq[Buff]): (Seq[(Int, Int)], Int, Int) = {
    val map: Seq[(Int, Int)] = (Config.maxPoint to Config.minPoint).foldLeft(Nil: Seq[(Int, Int)])((m, i) => (i, Cards.count(c => c.Point == i)) +: m)
    val dCardP = Config.minPoint - 1
    val xCardP = Config.maxPoint + 1

    (map, Cards.count(x => x.Point <= dCardP), Cards.count(x => x.Point >= xCardP))
  }

  def GetPoint(card: Card, buffs: Seq[Buff]): Int = card.Point //TODO buffsChangePoint

  //输入一个牌组，获得此牌组所有的可能的shape形式，并得知余下多少牌和x牌
  def genAllAllowedShapes(cards: Seq[Card], buffs: Seq[Buff]): Seq[Shape] = {
    val cardNum = cards.size
    val (pointSeq, d, x): (Seq[(Int, Int)], Int, Int) = genPointMapAndSpecial(cards, buffs)

    val shapes = pointSeq.foldLeft(Nil: Seq[Shape])((seq, ii) => {
      val (point, _) = ii
      val tempSeq = (Config.minLength to Config.maxLength).foldLeft(Nil: Seq[Shape])((Seq, l) => {
        val tuples: Seq[(Int, Int)] = sliceAPointSeq(point, l, pointSeq)
        val (_, maxH) = tuples.maxBy(w => w._2)
        val hTempSeq = (maxH + x to 1).foldLeft(Nil: Seq[Shape])((hSeq, i) => {
          val fillNeed = i * l - tuples.foldLeft(0)((sum, o) => sum + o._2)
          if (fillNeed <= x) {
            Shape(point, i, l, cardNum - i * l + d, x - fillNeed) +: hSeq
          }
          else hSeq
        })
        Seq ++ hTempSeq
      })
      tempSeq ++ seq
    })
    shapes
  }

  def genBiggestShape(cards: Seq[Card], buffs: Seq[Buff], extraAllowNum: Int): Option[Shape] = cards match {
    case Nil => None
    case _ => {
      val bigShape = genAllAllowedShapes(cards, buffs: Seq[Buff]).filter(shape => shape.extraNum <= extraAllowNum).maxBy(aShape => (aShape.height * aShape.length, aShape.keyPoint, aShape.height)) //获得一组牌最大的shape，
      val backShape = Shape(bigShape.keyPoint, bigShape.height, bigShape.length, 0, 0)
      Some(backShape)
    }
  }

  def sliceAPointSeq(point: Int, length: Int, pointSeq: Seq[(Int, Int)]): Seq[(Int, Int)] =
    point match {
      case p if p == Config.maxPoint => pointSeq.slice(Config.maxPoint - p, length + Config.maxPoint - p) //如果p为最大点，则从0到length切割
      case p if p >= length => pointSeq.slice(Config.maxPoint - p, length + Config.maxPoint - p) //如果p大于length，从倒数p点切割l长度
      case p if p < length && length < Config.maxLength => pointSeq.filter(v => v._1 <= p || v._1 > p - length + Config.maxPoint) //长度大于p，并且长度小于最大长度10，删选出1到p 再从10倒数l-p个
      case p if p < length && length == Config.maxLength => Nil: Seq[(Int, Int)] //p小于10 但长度为10 与以前取得的重复，所以不计算
    }

  //判断一组牌是否可以针对对应的shape非炸弹出牌

  def canShapeCounter(cards: Seq[Card], shape: Option[Shape], playerBuffs: Seq[Buff]): Option[Shape] = shape match { //某手出牌可以压住对手牌，返回None为不可压制，其他shape 为可以压制 传入BUff
    case None => genBiggestShape(cards, playerBuffs, 0)
    case a_shape if a_shape.get.keyPoint == Config.maxPoint => None
    case aShape if aShape.get.height < 0 => genBiggestShape(cards, playerBuffs, aShape.get.extraNum)
    case _ =>
      val cardNum = cards.size
      val h = shape.get.height
      val l = shape.get.length
      val ex = shape.get.extraNum
      val (pointSeq, d, x) = genPointMapAndSpecial(cards, playerBuffs)
      val tempShape =
        (Config.maxPoint to shape.get.keyPoint + 1).foldLeft(None: Option[Shape])((maybeShape, p) => {
          val tuples = sliceAPointSeq(p, l, pointSeq)
          val fillNeed = h * l - tuples.foldLeft(0)((sum, o) => sum + o._2)
          if (fillNeed == x && cardNum - h * l + d <= ex && maybeShape.isEmpty) {
            Some(Shape(p, h, l, 0, 0))
          }
          else maybeShape
        })
      tempShape
  }

  def canMultiShapeCounter(cards: Seq[Card], shapes: Seq[Option[Shape]], playerBuffs: Seq[Buff]): Option[Shape] = {
    val results = shapes.map(aShape => canShapeCounter(cards, aShape, playerBuffs)).filter(_.isDefined)
    results match {
      case Nil => None
      case rs => rs.maxBy(aShape => {
        val shapeGet = aShape.get
        (shapeGet.height * shapeGet.length, shapeGet.keyPoint, shapeGet.height)
      })
    }
  }


  def fillAShape(cards: Seq[Card], shape: Shape): Set[Int] = { //通过一个Shape，挑选对应的牌打出，用于自动托管
    //TODO 选牌逻辑
    Set(1, 2, 3)
  }
}