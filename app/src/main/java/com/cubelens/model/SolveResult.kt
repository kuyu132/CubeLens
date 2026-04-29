package com.cubelens.model

import com.cubelens.solver.Move

data class SolveResult(
  val moves: List<Move>,
  val errorMessage: String? = null,
)

