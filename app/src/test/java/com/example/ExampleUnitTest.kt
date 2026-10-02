package com.shopvion.flowgridgame

import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun test_all_100_levels_dynamically_loaded_by_id() {
    val layouts = mutableSetOf<String>()
    for (i in 1..100) {
      val lvl = LevelRepository.getLevel(i)
      assertEquals(i, lvl.levelNumber)

      // Verify grid scaling by ID
      val expectedGridSize = when {
        i <= 20 -> 5
        i <= 40 -> 6
        i <= 60 -> 7
        i <= 80 -> 8
        i <= 90 -> 9
        else -> 10
      }
      assertEquals(expectedGridSize, lvl.gridSize)
      assertEquals(expectedGridSize, LevelRepository.getGridSize(i))

      // Verify color count scaling by ID
      val expectedColors = when {
        i <= 20 -> 4
        i <= 40 -> 5
        i <= 60 -> 6
        i <= 80 -> 7
        else -> 8
      }
      assertEquals(expectedColors, lvl.colorPairs.size)
      assertEquals(expectedColors, LevelRepository.getColorCount(i))

      // Verify endpoints uniqueness
      val dots = lvl.colorPairs.flatMap { listOf(it.start, it.end) }
      val uniqueDots = dots.toSet()
      assertEquals("Level $i has duplicate endpoints!", dots.size, uniqueDots.size)

      val key = lvl.colorPairs.joinToString(";") { "${it.colorId}:${it.start.x},${it.start.y}-${it.end.x},${it.end.y}" }
      layouts.add(key)

      // Check if solution covers all cells
      val coveredCells = lvl.colorPairs.flatMap { it.solutionPath }.toSet()
      assertEquals("Level $i solution does not cover 100% cells!", lvl.totalCells, coveredCells.size)
    }
    // All 100 levels must have 100% unique layouts
    assertEquals(100, layouts.size)
  }
}
