package com.horizen.fixtures.sidechainblock.generation

import java.util.Random

case class GenerationRules(forgingBoxesToAdd: Set[SidechainForgingData] = Set(),
                          forgingBoxesToSpent: Set[SidechainForgingData] = Set(),
                          mcReferenceIsPresent: Option[Boolean] = None,
                          corruption: CorruptedGenerationRules = CorruptedGenerationRules.emptyCorruptedGenerationRules
                         ) {
  def isCorrupted: Boolean = corruption == CorruptedGenerationRules.emptyCorruptedGenerationRules
}

object GenerationRules {
  def generateCorrectGenerationRules(rnd: Random, generator: SidechainBlocksGenerator): GenerationRules = {

    val allNotSpentForgerData: Set[SidechainForgingData] = generator.getNotSpentBoxes
    val addForgingData: Set[SidechainForgingData] =
      if (allNotSpentForgerData.size > 100) {
        Set(SidechainForgingData.generate(rnd, Math.abs(rnd.nextInt(1000000))))
      }
      else {
        Set(SidechainForgingData.generate(rnd, Math.abs(rnd.nextInt(1000000))), SidechainForgingData.generate(rnd, Math.abs(rnd.nextInt(1000000))))
      }

    val removedForgingData: Set[SidechainForgingData] =
      if (rnd.nextBoolean()) {
        Set(allNotSpentForgerData.toSeq(rnd.nextInt(allNotSpentForgerData.size)))
      }
      else {
        val deleteSize = if (allNotSpentForgerData.size > 100) 3 else 1
        allNotSpentForgerData.toSeq.sortBy(_.forgerBox.value())(Ordering[Long].reverse).take(deleteSize).toSet
      }

    require((removedForgingData -- allNotSpentForgerData).isEmpty)

    GenerationRules(forgingBoxesToAdd = addForgingData, forgingBoxesToSpent = removedForgingData)
  }
}
