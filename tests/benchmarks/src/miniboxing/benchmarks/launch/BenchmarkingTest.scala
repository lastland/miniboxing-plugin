package miniboxing.benchmarks.launch

import scalameter.ScalameterBenchTest
import tests._

object BenchmarkingTest extends ScalameterBenchTest
                           with GenericBenchTest
                           with SpecializedBenchTest
                           with HardcodedMiniboxingSimpleFS // fullswitch
                           with HardcodedMiniboxingSimpleSS // semiswitch
                           with HardcodedMiniboxingSimpleDT // decision trees
                           with HardcodedMiniboxingSimpleLI // linear
                           with HardcodedMiniboxingSimpleNI // no inline
                           with HardcodedMiniboxingSimpleCL
                           with HardcodedMiniboxingSimple_JavaRT_1 // new Java runtime
                           with HardcodedMiniboxingSimple_JavaRT_2 // new Java runtime
                           with HardcodedMiniboxingDispatcherBenchTest
                           with HardcodedMiniboxingDispatcherBenchTestCL
                           with IdealBenchTest
                           with Serializable{

  // the number of independent samples to use
  lazy val sampleCount = 20

  // the test size
  lazy val testSizes = {
    //List(1000, 2000, 3000)
    List(1000000, 2000000, 3000000)
//    List(1000)
  }
  def megamorphicTestSize = 200000

  // run the tests:
  testIdeal()
  testHardcodedMiniboxingDispatch(false)
  testHardcodedMiniboxingDispatch(true)
  testHardcodedMiniboxingDispatchClassLoader(false)
  testHardcodedMiniboxingDispatchClassLoader(true)
  testHardcodedMiniboxingSimpleFS(false)
  testHardcodedMiniboxingSimpleFS(true)
  testHardcodedMiniboxingSimpleSS(false)
  testHardcodedMiniboxingSimpleSS(true)
  testHardcodedMiniboxingSimpleDT(false)
  testHardcodedMiniboxingSimpleDT(true)
  testHardcodedMiniboxingSimpleLI(false)
  testHardcodedMiniboxingSimpleLI(true)
  testHardcodedMiniboxingSimpleNI(false)
  testHardcodedMiniboxingSimpleNI(true)
  testHardcodedMiniboxingSimpleClassLoader(false)
  testHardcodedMiniboxingSimpleClassLoader(true)
  testHardcodedMiniboxingSimple_JavaRT_1(false, Nil)
  testHardcodedMiniboxingSimple_JavaRT_1(false, List("-XX:MaxInlineSize=120"))
  testHardcodedMiniboxingSimple_JavaRT_1(true, Nil)
  testHardcodedMiniboxingSimple_JavaRT_1(true, List("-XX:MaxInlineSize=120"))
  testHardcodedMiniboxingSimple_JavaRT_2(false)
  testHardcodedMiniboxingSimple_JavaRT_2(true)
  testSpecialized(false)
  testSpecialized(true)
  testGeneric(false)
  testGeneric(true)

  // forced print results
  printResults()
}


