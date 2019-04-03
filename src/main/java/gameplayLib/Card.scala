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


class CardEffect {


}

case class CommonValue(id: Int, level: Int, nowPoint: Int, copy: Boolean) //卡牌的一般属性 id 牌的唯一id nowPoint为当前点数，大于10点可以当作任意点数，小于1点只能当作单独牌出，copy为此牌是否为复制牌

case class Skill(tricks: Seq[Trick], effects: Seq[CardEffect])

case class Shape(keyPoint: Int, height: Int, length: Int, extraNum: Int, fillBlankRestNum: Int)

case class Card(standard: CommonValue, owner: Option[String], skill: Option[Skill])

object Card {
  def sortCard(Cards: Seq[Card]): Seq[Card] = Cards.sortWith(compareCardLessThan) //  按点数排列卡牌，从小到大排列，某些技能用到此功能

  def genPointMapAndSpecial(Cards: Seq[Card]): (Seq[(Int, Int)], Int, Int) = {
    val map: Seq[(Int, Int)] = (Config.maxPoint to Config.minPoint).foldLeft(Nil: Seq[(Int, Int)])((m, i) => (i, Cards.count(c => c.standard.nowPoint == i)) +: m)
    val dCard = Config.minPoint - 1
    val xCard = Config.maxPoint + 1

    (map, Cards.count(x => x.standard.nowPoint <= dCard), Cards.count(x => x.standard.nowPoint >= xCard))
  }

  def compareCardLessThan(a: Card, b: Card): Boolean = a.standard.nowPoint < b.standard.nowPoint || (a.standard.nowPoint == b.standard.nowPoint && a.standard.id == b.standard.id)


  //输入一个牌组，获得此牌组所有的可能的shape形式，并得知余下多少牌和x牌
  def genAllShapes(cards: Seq[Card]): Seq[Shape] = {
    val cardNum = cards.size
    val (pointSeq, d, x) = genPointMapAndSpecial(cards)

    val shapes = pointSeq.foldLeft(Nil: Seq[Shape])((seq, i) => {
      val (point, _) = i
      var tempSeq = Nil: Seq[Shape]
      for (l <- Config.minLength to Config.maxLength) {
        val tuples: Seq[(Int, Int)] = sliceAPointSeq(point, l, pointSeq)
        val (_, maxH) = tuples.maxBy(w => w._2)
        for (i <- maxH + x to 1) {
          val fillNeed = i * l - tuples.foldLeft(0)((sum, o) => sum + o._2)
          if (fillNeed <= x) {
            tempSeq = Shape(point, i, l, cardNum - i * l + d, x - fillNeed) +: tempSeq
          }
          else tempSeq
        }
      }
      tempSeq ++ seq
    })
    shapes
  }

  def genBiggestShape(cards: Seq[Card]): Shape = genAllShapes(cards).maxBy(aShape => (aShape.height * aShape.length, aShape.keyPoint, aShape.height)) //获得一组牌最大的shape，

  def sliceAPointSeq(point: Int, length: Int, pointSeq: Seq[(Int, Int)]): Seq[(Int, Int)] =
    point match {
      case p if p == Config.maxPoint => pointSeq.slice(Config.maxPoint - p, length + Config.maxPoint - p) //如果p为最大点，则从0到length切割
      case p if p >= length => pointSeq.slice(Config.maxPoint - p, length + Config.maxPoint - p) //如果p大于length，从倒数p点切割l长度
      case p if p < length && length < Config.maxLength => pointSeq.filter(v => v._1 <= p || v._1 > p - length + Config.maxPoint) //长度大于p，并且长度小于最大长度10，删选出1到p 再从10倒数l-p个
      case p if p < length && length == Config.maxLength => Nil: Seq[(Int, Int)] //p小于10 但长度为10 与以前取得的重复，所以不计算
    }

  def canShapeCounter(cards: Seq[Card], shape: Shape): Seq[Shape] = shape.keyPoint match { //XX出牌可以压住对手牌，返回Nil为不可压制，其他为可以压制
    case Config.maxPoint => Nil: Seq[Shape]
    case _ =>
      val cardNum = cards.size
      val h = shape.height
      val l = shape.length
      val (pointSeq, d, x) = genPointMapAndSpecial(cards)
      var tempSeq = Nil: Seq[Shape]
      for (p <- Config.maxPoint to shape.keyPoint + 1) {
        val tuples = sliceAPointSeq(p, l, pointSeq)
        val fillNeed = h * l - tuples.foldLeft(0)((sum, o) => sum + o._2)
        if (fillNeed == x && cardNum - h * l + d == 0) {
          tempSeq = Shape(p, h, l, 0, 0) +: tempSeq
        }
      }
      tempSeq

  }

  //判断一组牌是否可以针对对应的shape非炸弹出牌


}