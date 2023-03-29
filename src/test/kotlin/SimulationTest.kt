import com.example.demo.QueueAlgoritm
import com.example.demo.Simulation
import org.junit.jupiter.api.Test

class SimulationTest {

    @Test
    fun `test`() {
        val simulation = Simulation(variansAnkomst = 4.0,
            algoritme = QueueAlgoritm.FOUR_STATUSES,
            gjennomsittForLedigLege = 5,
            gjennomsittligIntervallAnkomst = 5,
            antallPasienter = 45,
            oransjePerDag = 2,
            status2Capacity = 1,
            status3Capacity = 1,
            randomSeed = 43)
        simulation.runSimulation()
    }
}