package gameplayLib

import scala.collection.immutable

object Config {
  val maxPoint = 10
  val minPoint = 1
  val minStraightNeed = 5
  val maxLength: Int = maxPoint - minPoint + 1
  val minLength = 1
  val levelArray: Array[(Int, Int)] = Array(
    (0, 1),
    (2, 2),
    (5, 3),
    (9, 4),
    (15, 5),
    (24, 6),
    (36, 7),
    (52, 8),
    (72, 9),
    (97, 10)
  )

  val initHitPoint = 1000


  val normalCards: Seq[Card] = (1 to 11).map(i => Card(i, i, i, i, CardStandType.Original, None, Nil: Seq[CardSkill], Nil))

  def genTestCharCards(cid: Int): Seq[Card] = {
    (1 to 10).map(i => Card(cid * 100 + i, i, i, i, CardStandType.Original, Some(cid), Nil, Nil))
  }

  val standardCIds: Array[Int] = Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

  val startBombNeedNum = 4
  val summonPoints: Seq[Int] = Seq(0, 10, 25, 50, 100)
  val exSummonMax: Int = 4

  val oneCardDamage: Int = 10


  val attrMap: Map[Int, (Seq[Int], Seq[Int])] = Map(
    1 -> (Seq(1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 8), Seq(1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 8)),
    2 -> (Seq(1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 8), Seq(1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 8)),
    3 -> (Seq(1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 8), Seq(1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 8))
  )

  val mapTest = attrMap.getOrElse(0, 0)

  val splitAtTest = Seq(1,2,3,4,5,6,7,8).splitAt(10)
}
