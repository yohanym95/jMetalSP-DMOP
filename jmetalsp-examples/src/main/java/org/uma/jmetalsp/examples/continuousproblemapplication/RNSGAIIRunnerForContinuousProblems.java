package org.uma.jmetalsp.examples.continuousproblemapplication;

import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.impl.crossover.SBXCrossover;
import org.uma.jmetal.operator.impl.mutation.PolynomialMutation;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistance;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.point.PointSolution;
import org.uma.jmetalsp.*;
import org.uma.jmetalsp.algorithm.rnsgaii.DynamicRNSGAII;
import org.uma.jmetalsp.algorithm.rnsgaii.DynamicRNSGAIIBuilder;
import org.uma.jmetalsp.consumer.ChartInDM2Consumer;
import org.uma.jmetalsp.consumer.LocalDirectoryOutputConsumer;
import org.uma.jmetalsp.examples.streamingdatasource.ComplexStreamingDataSourceFromKeyboard;
import org.uma.jmetalsp.examples.streamingdatasource.SimpleStreamingCounterDataSource;
import org.uma.jmetalsp.impl.DefaultRuntime;
import org.uma.jmetalsp.observeddata.AlgorithmObservedData;
import org.uma.jmetalsp.observeddata.ObservedValue;
import org.uma.jmetalsp.observer.impl.DefaultObservable;
import org.uma.jmetalsp.problem.fda.FDA2;
import org.uma.jmetalsp.qualityindicator.CoverageFront;
import org.uma.jmetalsp.util.restartstrategy.RestartStrategy;
import org.uma.jmetalsp.util.restartstrategy.impl.CreateNRandomSolutions;
import org.uma.jmetalsp.util.restartstrategy.impl.RemoveNRandomSolutions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Example of SparkSP application.
 * Features:
 * - Algorithm: InDM2
 * - Problem: Any of the FDA familiy
 * - Default streaming runtime (Spark is not used)
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public class RNSGAIIRunnerForContinuousProblems {

  public static void main(String[] args) throws IOException, InterruptedException {
    // STEP 1. Create the problem
    DynamicProblem<DoubleSolution, ObservedValue<Integer>> problem =
            new FDA2();

    // STEP 2. Create and configure the algorithm
    List<Double> referencePoint = new ArrayList<>();
    referencePoint.add(0.0);
    referencePoint.add(0.0);
    /*referencePoint.add(0.5);
    referencePoint.add(0.5);
    referencePoint.add(1.0);
    referencePoint.add(0.0);
    referencePoint.add(1.0);
    referencePoint.add(1.0);
    referencePoint.add(0.0);
    referencePoint.add(1.0);
    referencePoint.add(1.0);
    referencePoint.add(1.0);
    referencePoint.add(0.1);
    referencePoint.add(0.9);
    referencePoint.add(0.9);
    referencePoint.add(0.7);*/

    CrossoverOperator<DoubleSolution> crossover = new SBXCrossover(0.9, 20.0);
    MutationOperator<DoubleSolution> mutation =
            new PolynomialMutation(1.0 / problem.getNumberOfVariables(), 20.0);
    double epsilon = 0.001D;
    InvertedGenerationalDistance<PointSolution> igd =
            new InvertedGenerationalDistance<>();
    CoverageFront<PointSolution> coverageFront = new CoverageFront<>(0.005,igd);
    DynamicRNSGAII<DoubleSolution> algorithm =   new DynamicRNSGAIIBuilder<>(crossover, mutation, new DefaultObservable<>(),referencePoint,epsilon,coverageFront)
            .setMaxEvaluations(50000)
            .setPopulationSize(100)
            .build(problem);

    algorithm.setRestartStrategy(new RestartStrategy<>(
            //new RemoveFirstNSolutions<>(50),
            //new RemoveNSolutionsAccordingToTheHypervolumeContribution<>(50),
            //new RemoveNSolutionsAccordingToTheCrowdingDistance<>(50),
            new RemoveNRandomSolutions<>(10),
            new CreateNRandomSolutions<>()));

    algorithm.setRestartStrategyForReferencePointChange(new RestartStrategy<>(
            new RemoveNRandomSolutions<>(10),
            new CreateNRandomSolutions<DoubleSolution>()));

    // STEP 3. Create a streaming data source for the problem and register
    StreamingDataSource<ObservedValue<Integer>> streamingDataSource =
            new SimpleStreamingCounterDataSource(2000) ;

    streamingDataSource.getObservable().register(problem);

    // STEP 4. Create a streaming data source for the algorithm
    StreamingDataSource<ObservedValue<List<Double>>> keyboardstreamingDataSource =
            new ComplexStreamingDataSourceFromKeyboard() ;

    // STEP 5. Create the data consumers
    DataConsumer<AlgorithmObservedData> localDirectoryOutputConsumer =
            new LocalDirectoryOutputConsumer<DoubleSolution>("outputdirectory") ;
    DataConsumer<AlgorithmObservedData> chartConsumer =
            new ChartInDM2Consumer<DoubleSolution>(algorithm.getName(), referencePoint,problem.getNumberOfObjectives(),problem.getName()) ;

    // STEP 6. Create the application and run
    JMetalSPApplication<
            DoubleSolution,
            DynamicProblem<DoubleSolution, ObservedValue<Integer>>,
            DynamicAlgorithm<List<DoubleSolution>, AlgorithmObservedData>> application;

    application = new JMetalSPApplication<>();

    application.setStreamingRuntime(new DefaultRuntime())
            .setProblem(problem)
            .setAlgorithm(algorithm)
            .addStreamingDataSource(streamingDataSource,problem)
            .addStreamingDataSource(keyboardstreamingDataSource,algorithm)
            .addAlgorithmDataConsumer(localDirectoryOutputConsumer)
            .addAlgorithmDataConsumer(chartConsumer)
            .run();
  }
}
