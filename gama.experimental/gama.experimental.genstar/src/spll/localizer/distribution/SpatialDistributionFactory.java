package spll.localizer.distribution;

import java.util.Collection;

import gama.core.metamodel.agent.IAgent;
import gama.core.metamodel.shape.IShape;
import gama.core.runtime.IScope;
import gama.core.util.IList;
import spll.localizer.constraint.SpatialConstraintMaxNumber;
import spll.localizer.distribution.function.AreaFunction;
import spll.localizer.distribution.function.CapacityFunction;
import spll.localizer.distribution.function.DistanceFunction;
import spll.localizer.distribution.function.GravityFunction;
import spll.localizer.distribution.function.ISpatialComplexFunction;
import spll.localizer.distribution.function.ISpatialEntityFunction;

/**
 * Build distribution to asses spatial entity probability to be bind with synthetic population entity.
 * This factory provide basic distribution creation methods together with example distributions:
 * 
 * <br/>
 * <ul>
 *  <li>Distribution based on simple function (lambda):</li>
 *  <ul>
 *   <li> according to area of spatial object: e.g. {@link #getAreaBasedDistribution()}
 *   <li> according to a predefined number: e.g. {@link #getCapacityBasedDistribution(SpatialConstraintMaxNumber)}
 *  </ul>
 *  <li>Distribution based on complex bi-function (lambda):</li>
 *  <ul>
 *   <li> according to the distance of spatial object from other places {@link #getDistanceBasedDistribution()} </li>
 *   <li> according to a given mass function {@link #getGravityModelDistribution(Collection, double, SpllEntity...)}
 *  </ul>
 * </ul>
 *  
 *  Defining custom {@link ISpatialEntityFunction} or {@link ISpatialComplexFunction} allows to define user made distribution
 *  <br/>
 *  
 * @author kevinchapuis
 *
 */
public class SpatialDistributionFactory {

	private static SpatialDistributionFactory sdf = new SpatialDistributionFactory();
	
	private SpatialDistributionFactory() {}
	
	public static SpatialDistributionFactory getInstance() {
		return sdf;
	}
	
	/**
	 * General factory method to create distribution based on a function that transposed spatial entity into a number.
	 * Provided example includes, area based distribution {@link #getAreaBasedDistribution()} and capacity based distribution
	 * {@link #getCapacityBasedDistribution(SpatialConstraintMaxNumber)}
	 * 
	 * @param function
	 * @return
	 */
	public <N extends Number, E extends IShape> ISpatialDistribution<IShape> getDistribution(ISpatialEntityFunction<N> function){
		return new BasicSpatialDistribution<N, IShape>(function);
	}
	
	/**
	 * General factory method to build distribution with cached candidates
	 * @see #getDistribution(ISpatialEntityFunction)
	 * 
	 * @param function
	 * @param candidates
	 * @return
	 */
	public <N extends Number, E extends IShape> ISpatialDistribution<IShape> getDistribution(ISpatialEntityFunction<N> function,
			IList<IShape> candidates){
		ISpatialDistribution<IShape> distribution = new BasicSpatialDistribution<>(function);
		distribution.setCandidate(candidates);
		return distribution;
	}
	

	/////////////////////////////////////////////////////////////////////////////////
	//							Example distribution                              //
	/////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * All provided spatial entities have the same probability - uniform distribution
	 * @return
	 */
	public <IAgent> ISpatialDistribution<IShape> getUniformDistribution(){
		return new UniformSpatialDistribution<>();
	}
	
	/**
	 * Probability is computed as a linear function of spatial entity area. That is,
	 * the bigger the are is, the bigger will be the probability to be located in.
	 * 
	 * @return
	 */
	public <IAgent> ISpatialDistribution<IShape> getAreaBasedDistribution(){
		return this.getDistribution(new AreaFunction());
	}
	
	/**
	 * @see #getAreaBasedDistribution()
	 * <p>
	 * adds cached candidates
	 * 
	 * @return
	 */
	public <IAgent> ISpatialDistribution<IShape> getAreaBasedDistribution(IScope scope, IList<IShape> candidates){
		return this.getDistribution(new AreaFunction(), candidates);
	}
	
	
	/**
	 * Probability is computed as a linear function of spatial entity capacity. This capacity
	 * is provided by {@code scNumber} argument and can be dynamically updated
	 * @param scNumber
	 * @return
	 */
	public <IAgent> ISpatialDistribution<IShape> getCapacityBasedDistribution(SpatialConstraintMaxNumber scNumber){
		return new BasicSpatialDistribution<>(new CapacityFunction(scNumber));
	}
	
	// ----------------------------------------------------------------------------------- //
	
	/**
	 * General factory method to create distribution based on biFunction implementation that transposed 
	 * @param function
	 * @return
	 */
	public <N extends Number> ISpatialDistribution getDistribution(ISpatialComplexFunction<N> function){
		return new ComplexSpatialDistribution<N>(function);
	}
	
	/**
	 * General factory method to build complex function based distribution with cached candidates
	 * 
	 * @see #getDistribution(ISpatialComplexFunction)
	 * 
	 * @param function
	 * @param candidates
	 * @return
	 */
	public <N extends Number> ISpatialDistribution getDistribution(ISpatialComplexFunction<N> function,
			IList<IShape> candidates){
		ISpatialDistribution distribution = new ComplexSpatialDistribution<N>(function);
		distribution.setCandidate(candidates);
		return distribution;
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	//							Example distribution                              //
	/////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Probability is computed as a linear function of distance between spatial and population entities
	 * @return
	 */
	public ISpatialDistribution getDistanceBasedDistribution(){
		return new ComplexSpatialDistribution<>(new DistanceFunction());
	}
	
	/**
	 * Gravity model that associate probability (the mass in gravity model) to each candidates according to gravity model
	 * 
	 * @param candidates
	 * @param frictionCoeff
	 * @param entities
	 * @return
	 */
	public ISpatialDistribution getGravityModelDistribution(
			IList<IShape> candidates, 
			double frictionCoeff,  
			IList<IAgent> entities){ 
		return new ComplexSpatialDistribution<>(new GravityFunction(candidates, frictionCoeff, entities));
	}
	
	/**
	 * Gravity model that associate probability (the mass in gravity model) to each candidates according to gravity model, 
	 * considering a given buffer around geometries
	 * 
	 * @param candidates
	 * @param frictionCoeff
	 * @param buffer
	 * @param entities
	 * @return 
	 */
	public ISpatialDistribution getGravityModelDistribution(
			IScope scope, IList<IShape> candidates, 
			double frictionCoeff, double buffer, IList<IAgent> entities){ 
		return new ComplexSpatialDistribution(new GravityFunction(scope, candidates, frictionCoeff, buffer, entities));
	}
	
}
