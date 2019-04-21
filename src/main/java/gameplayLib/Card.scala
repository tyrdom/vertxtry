package gameplayLib

import scala.util.Random

//卡牌的一般属性 id 牌的唯一id nowPoint为当前点数，大于10点可以当作任意点数，小于1点只能当作单独牌出，copy为此牌是否为复制牌


case class Shape(keyPoint: Int, height: Int, length: Int, extraNum: Int, fillBlankRestNum: Int)


case class Card(id: Int, level: Int, nowPoint: Int, copy: Boolean, ownerCharacterId: Option[Int], skill: Seq[Skill])


object Card {
  def sortCard(Cards: Seq[Card]): Seq[Card] = Cards.sortWith(compareCardLessThan) //  按点数排列卡牌，从小到大排列，某些技能用到此功能

  def shuffleCard(Cards: Seq[Card]): Seq[Card] = Random.shuffle(Cards)

  def genPointMapAndSpecial(Cards: Seq[Card]): (Seq[(Int, Int)], Int, Int) = {
    val map: Seq[(Int, Int)] = (Config.maxPoint to Config.minPoint).foldLeft(Nil: Seq[(Int, Int)])((m, i) => (i, Cards.count(c => c.nowPoint == i)) +: m)
    val dCardP = Config.minPoint - 1
    val xCardP = Config.maxPoint + 1

    (map, Cards.count(x => x.nowPoint <= dCardP), Cards.count(x => x.nowPoint >= xCardP))
  }

  def compareCardLessThan(a: Card, b: Card): Boolean = a.nowPoint < b.nowPoint || (a.nowPoint == b.nowPoint && a.id == b.id)


  //输入一个牌组，获得此牌组所有的可能的shape形式，并得知余下多少牌和x牌
  def genAllShapes(cards: Seq[Card]): Seq[Shape] = {
    val cardNum = cards.size
    val (pointSeq, d, x) = genPointMapAndSpecial(cards)

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

  def genBiggestShape(cards: Seq[Card]): Option[Shape] = cards match {
    case Nil => None
    case _ => Some(genAllShapes(cards).maxBy(aShape => (aShape.height * aShape.length, aShape.keyPoint, aShape.height))) //获得一组牌最大的shape，
  }

  def sliceAPointSeq(point: Int, length: Int, pointSeq: Seq[(Int, Int)]): Seq[(Int, Int)] =
    point match {
      case p if p == Config.maxPoint => pointSeq.slice(Config.maxPoint - p, length + Config.maxPoint - p) //如果p为最大点，则从0到length切割
      case p if p >= length => pointSeq.slice(Config.maxPoint - p, length + Config.maxPoint - p) //如果p大于length，从倒数p点切割l长度
      case p if p < length && length < Config.maxLength => pointSeq.filter(v => v._1 <= p || v._1 > p - length + Config.maxPoint) //长度大于p，并且长度小于最大长度10，删选出1到p 再从10倒数l-p个
      case p if p < length && length == Config.maxLength => Nil: Seq[(Int, Int)] //p小于10 但长度为10 与以前取得的重复，所以不计算
    }

  //判断一组牌是否可以针对对应的shape非炸弹出牌

  def canShapeCounter(cards: Seq[Card], shape: Option[Shape]): Option[Shape] = shape match { //XX出牌可以压住对手牌，返回Nil为不可压制，其他为可以压制
    case None => genBiggestShape(cards)
    case a_shape if a_shape.get.keyPoint == Config.maxPoint => None
    case _ =>
      val cardNum = cards.size
      val h = shape.get.height
      val l = shape.get.length
      val (pointSeq, d, x) = genPointMapAndSpecial(cards)
      val tempShape =
        (Config.maxPoint to shape.get.keyPoint + 1).foldLeft(None: Option[Shape])((maybeShape, p) => {
          val tuples = sliceAPointSeq(p, l, pointSeq)
          val fillNeed = h * l - tuples.foldLeft(0)((sum, o) => sum + o._2)
          if (fillNeed == x && cardNum - h * l + d == 0 && maybeShape.isEmpty) {
            Some(Shape(p, h, l, 0, 0))
          }
          else maybeShape
        })
      tempShape
  }

  def fillAShape(cards: Seq[Card], shape: Shape): Set[Int] = { //通过一个Shape，挑选对应的牌打出，用于自动托管
    //TODO 选牌逻辑
    Set(1, 2, 3)
  }


}