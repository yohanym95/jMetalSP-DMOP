package org.uma.jmetalsp.problem.fda;

import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetalsp.observeddata.ObservedValue;
import org.uma.jmetalsp.observer.Observable;
import org.uma.jmetalsp.observer.impl.DefaultObservable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Cristóbal Barba <cbarba@lcc.uma.es>
 */
public class FDA4 extends FDA implements Serializable {

  private boolean theProblemHasBeenModified;
  private final int M = 3;

  public FDA4(Observable<ObservedValue<Integer>> observable){
    this(12,3, observable);
  }

  public FDA4() {
    this(new DefaultObservable<>()) ;
  }

  public FDA4(Integer numberOfVariables, Integer numberOfObjectives, Observable<ObservedValue<Integer>> observer) throws JMetalException {
    super(observer) ;
    setNumberOfVariables(numberOfVariables);
    setNumberOfObjectives(numberOfObjectives);
    setName("FDA4");

    List<Double> lowerLimit = new ArrayList<>(getNumberOfVariables());
    List<Double> upperLimit = new ArrayList<>(getNumberOfVariables());

    for (int i = 0; i < getNumberOfVariables(); i++) {
      lowerLimit.add(0.0);
      upperLimit.add(1.0);
    }

    setLowerLimit(lowerLimit);
    setUpperLimit(upperLimit);
    time=1.0d;
    theProblemHasBeenModified=false;
  }
  @Override
  public boolean hasTheProblemBeenModified() {
    return theProblemHasBeenModified;
  }

  @Override
  public void reset() {
    theProblemHasBeenModified = false ;
  }

  @Override
  public void evaluate(DoubleSolution solution) {
    double[] f = new double[getNumberOfObjectives()];
    double g = this.evalG(solution, M-1);
    f[0] = this.evalF1(solution, g);
    f[1] = evalFK(solution,g,2);
    f[2] = evalFM(solution,g);
    for (int i = 0; i < solution.getNumberOfObjectives() ; i++) {
      solution.setObjective(i,f[i]);
    }
  }

  private double evalF1(DoubleSolution solution,double g){
    double f=1.0d +g;
    double mult=1.0d;
    for (int i = 1; i <= M-1; i++) {
      mult*=Math.cos(solution.getVariableValue(i-1)*Math.PI/2.0d);
    }
    return f*mult;
  }
  private double evalFK(DoubleSolution solution,double g,int k){
    double f= 1.0d + g;
    double mult=1.0d;
    double aux=Math.sin((solution.getVariableValue(M-k)*Math.PI)/2.0d);
    for (int i = 1; i <= M-k; i++) {
      mult*=Math.cos(solution.getVariableValue(i-1)*Math.PI/2.0d);
    }
    mult*=aux;
    return f*mult;
  }


  /**
   * Returns the value of the FDA4 function G.
   *
   * @param solution Solution
   */
  private double evalG(DoubleSolution solution,int limitInf) {
    double g = 0.0d;
    double Gt=Math.abs(Math.sin(0.5d*Math.PI*time));
    for (int i = limitInf; i < solution.getNumberOfVariables(); i++) {
      g += Math.pow((solution.getVariableValue(i)-Gt), 2.0d);
    }
    return g;
  }

  private double evalFM(DoubleSolution solution,double g){
    double fm = 1.0d+g;
    fm *= Math.sin(solution.getVariableValue(0)*Math.PI/2);
    return fm;
  }
}
