package com.shopvion.flowgridgame

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FlowGrid", appName)
  }

  @Test
  fun `verify level generation coverage`() {
    for (lvl in 1..20) {
      val levelData = LevelRepository.getLevel(lvl)
      assertEquals(5, levelData.gridSize)
      assertEquals(4, levelData.colorPairs.size)
      val occupied = levelData.colorPairs.flatMap { it.solutionPath }.toSet()
      assertEquals(25, occupied.size)
    }
  }

  @Test
  fun `verify preferences manager operations`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = PreferencesManager(context)
    prefs.resetAllProgress()
    assertEquals(1, prefs.getUnlockedLevel())
    prefs.unlockLevel(5)
    assertEquals(5, prefs.getUnlockedLevel())
  }

  @Test
  fun `verify win condition evaluation logic and bidirectional connection`() {
    val levelData = LevelRepository.getLevel(1)
    val paths = mutableMapOf<Int, List<GridPoint>>()

    // Test bidirectional connection check for each color pair
    levelData.colorPairs.forEachIndexed { idx, pair ->
      val forwardPath = pair.solutionPath
      val reversePath = pair.solutionPath.reversed()

      assertTrue(isColorPairConnected(pair, forwardPath))
      assertTrue(isColorPairConnected(pair, reversePath))

      // Alternate directions to test mixed flow orientations
      paths[pair.colorId] = if (idx % 2 == 0) forwardPath else reversePath
    }

    val connectedCount = countConnectedFlows(levelData, paths)
    assertEquals(levelData.colorPairs.size, connectedCount)

    val occupiedCells = calculateOccupiedCells(paths)
    assertEquals(levelData.totalCells, occupiedCells.size)

    val isWon = (connectedCount == levelData.colorPairs.size) && (occupiedCells.size == levelData.totalCells)
    assertTrue(isWon)
  }

  @Test
  fun `verify levels have unique dot positions and proper scaling across level IDs`() {
    val level1 = LevelRepository.getLevel(1)
    val level2 = LevelRepository.getLevel(2)
    val level3 = LevelRepository.getLevel(3)

    val endpoints1 = level1.colorPairs.map { it.start to it.end }
    val endpoints2 = level2.colorPairs.map { it.start to it.end }
    val endpoints3 = level3.colorPairs.map { it.start to it.end }

    // Must be distinct layouts
    assertTrue(endpoints1 != endpoints2)
    assertTrue(endpoints2 != endpoints3)
    assertTrue(endpoints1 != endpoints3)

    // Check scaling across tiers
    assertEquals(5, LevelRepository.getLevel(1).gridSize)
    assertEquals(4, LevelRepository.getLevel(1).colorPairs.size)

    assertEquals(6, LevelRepository.getLevel(25).gridSize)
    assertEquals(5, LevelRepository.getLevel(25).colorPairs.size)

    assertEquals(7, LevelRepository.getLevel(45).gridSize)
    assertEquals(6, LevelRepository.getLevel(45).colorPairs.size)

    assertEquals(8, LevelRepository.getLevel(65).gridSize)
    assertEquals(7, LevelRepository.getLevel(65).colorPairs.size)

    assertEquals(9, LevelRepository.getLevel(85).gridSize)
    assertEquals(8, LevelRepository.getLevel(85).colorPairs.size)

    assertEquals(10, LevelRepository.getLevel(95).gridSize)
    assertEquals(8, LevelRepository.getLevel(95).colorPairs.size)
  }
}
