package com.rmp.paprika.services

import com.google.ortools.Loader
import com.google.ortools.linearsolver.MPConstraint
import com.google.ortools.linearsolver.MPObjective
import com.google.ortools.linearsolver.MPSolver
import com.google.ortools.linearsolver.MPVariable
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.korm.column.Column
import com.rmp.paprika.dto.mpsolver.ConstraintDto

/*

    Solver service which wraps the MPSolver library by google
    Here we load the variables, constraints and data into the Solver Model and then get the results

    The solving don`t suspends, we wait for it in real time.
 */
class MPSolverService internal constructor() {
    enum class SolveDirection {
        MINIMIZE, MAXIMIZE
    }

    companion object {
        fun initSolver(init: MPSolverService.() -> Unit): MPSolverService {
            val solver = MPSolverService()
            solver.apply(init)
            return solver
        }
    }

    private var constraints: MutableList<ConstraintDto> = mutableListOf()
    private var solveDirection: SolveDirection = SolveDirection.MINIMIZE
    private var itemsInAnswer: Int = 0
    private var data: List<Row> = listOf()
    private lateinit var solver: MPSolver
    private lateinit var objectiveKey: Column<Number>

    infix fun onDirection(solveDirection: SolveDirection) {
        this.solveDirection = solveDirection
    }
    infix fun answersCount(answersCount: Int) {
        this.itemsInAnswer = answersCount
    }
    infix fun setConstraint(inject: ConstraintDto.() -> Unit) {
        val constraint = ConstraintDto().apply(inject)
        println("New constraint $constraint")
        this.constraints.add(constraint)
    }
    infix fun constraints(constraints: List<ConstraintDto>) {
        this.constraints.addAll(constraints)
    }
    infix fun onData(data: List<Row>) {
        this.data = data
    }
    infix fun <T : Number> withObjective(objectiveKey: Column<T>) {
        this.objectiveKey = objectiveKey as Column<Number>
    }

    private fun mpVariables(): List<MPVariable> {
        println("Start variables setup")
        val vars = data.map {
            solver.makeBoolVar(it[DishModel.name])
        }
        println("Variables setup finished")
        return vars
    }

    private fun mpConstraints(): List<MPConstraint> {
        println("Start constraints setup")
        val consts = constraints.map {
            solver.makeConstraint(it.bottom, it.top, it.name)
        }
        println("Constraints setup finished")
        return consts
    }

    private fun mpObjective() = solver.objective().apply {
            if (solveDirection == SolveDirection.MAXIMIZE)
                setMaximization()
            else
                setMinimization()
        }

    private fun setCoefficients(constraints: List<MPConstraint>, variables: List<MPVariable>, objective: MPObjective) {
        variables.mapIndexed { varIndex, variable ->
            val dish = data[varIndex]
            constraints.mapIndexed { constIndex, mpConstraint -> run {
                    val constraint = this.constraints[constIndex]
                    if (constraint.bool)
                        mpConstraint.setCoefficient(variable, 1.0)
                    else
                        mpConstraint.setCoefficient(variable, dish[constraint.modelKey] as Double)
                }
            }
            objective.setCoefficient(variable, dish[objectiveKey].toDouble())
        }
    }

    private fun initialize(): List<MPVariable> {
        if (itemsInAnswer > 0)
            setConstraint {
                name = "count"
                bool = true
                top = itemsInAnswer + 0.1
                bottom = itemsInAnswer - 0.1
            }

        Loader.loadNativeLibraries()
        this.solver = MPSolver.createSolver("SCIP")
//        this.solver.setNumThreads(4)
        println("Start variables and constraints calculation")
        val variables = mpVariables()

        setCoefficients(mpConstraints(), variables, mpObjective())
        println("Variables have been set")

        return variables
    }

    fun solve(): List<Row> {
        val varsOnSolve = initialize()
        println("Start solving. Data size: ${data.size}")
        val result = solver.solve()

        // Check that the problem has an optimal solution.
        if (result != MPSolver.ResultStatus.OPTIMAL) {
            println("The problem does not have an optimal solution!")
            if (result == MPSolver.ResultStatus.FEASIBLE) {
                println("A potentially suboptimal solution was found.")
            } else {
                println("The solver could not solve the problem.")
                return listOf()
            }
        }

        val answers = varsOnSolve.filter {
            it.solutionValue() > 0.0
        }

        return answers.map {
            data[varsOnSolve.indexOf(it)]
        }
    }

}