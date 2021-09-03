package org.uma.jmetalsp.examples.continuousproblemapplication;

import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.impl.crossover.SBXCrossover;
import org.uma.jmetal.operator.impl.mutation.PolynomialMutation;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistance;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.archivewithreferencepoint.ArchiveWithReferencePoint;
import org.uma.jmetal.util.archivewithreferencepoint.impl.CrowdingDistanceArchiveWithReferencePoint;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;
import org.uma.jmetal.util.point.PointSolution;
import org.uma.jmetalsp.*;
import org.uma.jmetalsp.algorithm.indm2.InDM2;
import org.uma.jmetalsp.algorithm.indm2.InDM2Builder;
import org.uma.jmetalsp.algorithm.smpso.InteractiveSMPSORP;
import org.uma.jmetalsp.consumer.ChartInDM2Consumer;
import org.uma.jmetalsp.consumer.LocalDirectoryOutputConsumer;
import org.uma.jmetalsp.examples.streamingdatasource.ComplexStreamingDataSourceFromKeyboard;
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
import java.util.Arrays;
import java.util.List;

/**
 * Example of SparkSP application. Features: - Algorithm: InDM2 - Problem: Any of the FDA familiy -
 * Default streaming runtime (Spark is not used)
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public class InDM2RunnerForContinuousAutoUpdateProblems {

  public static void main(String[] args) throws IOException, InterruptedException {
    // STEP 1. Create the problem
    DynamicProblem<DoubleSolution, ObservedValue<Integer>> problem =
        // new DF1();
        new FDA2();

    List<Double> referencePoint = Arrays.asList(0.0, 0.0);
    List<List<Double>> referencePoints;
    referencePoints = new ArrayList<>();

    referencePoints.add(referencePoint);

    double mutationProbability = 1.0 / problem.getNumberOfVariables();
    double mutationDistributionIndex = 20.0;
    MutationOperator<DoubleSolution> mutation =
        new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    int maxIterations = 550000;
    int swarmSize = 100;

    List<ArchiveWithReferencePoint<DoubleSolution>> archivesWithReferencePoints = new ArrayList<>();

    for (int i = 0; i < referencePoints.size(); i++) {
      archivesWithReferencePoints.add(
          new CrowdingDistanceArchiveWithReferencePoint<DoubleSolution>(
              swarmSize / referencePoints.size(), referencePoints.get(i)));
    }

    CrossoverOperator<DoubleSolution> crossover = new SBXCrossover(0.9, 20.0);

    InteractiveAlgorithm<DoubleSolution, List<DoubleSolution>> iSMPSORP =
        new InteractiveSMPSORP(
            problem,
            swarmSize,
            archivesWithReferencePoints,
            referencePoints,
            mutation,
            maxIterations,
            0.0,
            1.0,
            0.0,
            1.0,
            2.5,
            1.5,
            2.5,
            1.5,
            0.1,
            0.1,
            -1.0,
            -1.0,
            new SequentialSolutionListEvaluator<>());

    double epsilon = 0.001D;

    InvertedGenerationalDistance<PointSolution> igd = new InvertedGenerationalDistance<>();
    CoverageFront<PointSolution> coverageFront = new CoverageFront<>(0.005, igd);
    InDM2<DoubleSolution> algorithm =
        new InDM2Builder<>(iSMPSORP, new DefaultObservable<>(), coverageFront)
            .setMaxIterations(100000)
            .setPopulationSize(100)
            .setAutoUpdate(true)
            .build(problem);
    int delay = 5000;
    algorithm.setRestartStrategy(
        new RestartStrategy<>(
            // new RemoveFirstNSolutions<>(50),
            // new RemoveNSolutionsAccordingToTheHypervolumeContribution<>(50),
            // new RemoveNSolutionsAccordingToTheCrowdingDistance<>(50),
            new RemoveNRandomSolutions(50), new CreateNRandomSolutions<DoubleSolution>()));

    algorithm.setRestartStrategyForReferencePointChange(
        new RestartStrategy<>(
            new RemoveNRandomSolutions<>(50), new CreateNRandomSolutions<DoubleSolution>()));

    // STEP 3. Create a streaming data source for the algorithm and register
    StreamingDataSource<ObservedValue<List<Double>>> keyboardstreamingDataSource =
        new ComplexStreamingDataSourceFromKeyboard();

    // STEP 4. Create the data consumers
    DataConsumer<AlgorithmObservedData> localDirectoryOutputConsumer =
        new LocalDirectoryOutputConsumer<DoubleSolution>(
            "outputdirectory-"
                + problem.getName()
                + "-"
                + algorithm.getName()
                + "-"
                + referenceName(referencePoint)); // algorithm
    DataConsumer<AlgorithmObservedData> chartConsumer =
        new ChartInDM2Consumer<DoubleSolution>(
            algorithm.getName(),
            referencePoint,
            problem.getNumberOfObjectives(),
            problem.getName());

    // STEP 5. Create the application and run
    JMetalSPApplication<
            DoubleSolution,
            DynamicProblem<DoubleSolution, ObservedValue<Integer>>,
            DynamicAlgorithm<List<DoubleSolution>, AlgorithmObservedData>>
        application;

    application = new JMetalSPApplication<>(problem, algorithm);

    application
        .setStreamingRuntime(new DefaultRuntime())
        .addStreamingDataSource(keyboardstreamingDataSource, algorithm)
        .addAlgorithmDataConsumer(localDirectoryOutputConsumer)
        .addAlgorithmDataConsumer(chartConsumer)
        .run();
  }

  private static String referenceName(List<Double> referencePoint) {
    String result = "(";
    for (Double ref : referencePoint) {
      result += ref + ",";
    }
    result = result.substring(0, result.length() - 1);
    result += ")";
    return result;
  }
}
