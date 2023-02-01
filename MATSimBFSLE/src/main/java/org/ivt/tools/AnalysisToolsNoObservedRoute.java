/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
@author      Matteo Felder
*/


package org.ivt.tools;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.Tuple;

public class AnalysisToolsNoObservedRoute {
		//variables
	
		private static final Logger log = Logger.getLogger(AnalysisToolsNoObservedRoute.class);
		
		protected final Tuple<Path,List<Path>> paths;
		
		
		// constructor
		
		public AnalysisToolsNoObservedRoute(Tuple<Path,List<Path>> paths) {
			this.paths = paths;
		}
		
		//public methods
		
		/**
		 *
		 * @param path
		 *            The path whose length we need to compute.
		 * @return
		 *            returns the length of the path
		 */
		
		public Double getPathLength(Path path) {
			Double length = path.links.stream().map(x -> x.getLength())
			  .reduce(Double::sum).orElse(0d);
			return length;
		}
		
		
		/**
		 * @param alternative
		 *            Any particular route (includes alternative = chosen route) in the choice set.
		 *
		 * @param paths
		 *            Set of alternatives: a tuple consisting of the least cost path, and a list of other alternatives.
		 * @return path_size_alt
		 *            Returns the Bierlaire-Ben-Akiva path size of the selected route with respect to the given set of alternatives and a chosen route.
		 */
		
		public final Double getBierlaireBenAkivaPathSize(Path alternative, Tuple<Path, List<Path>> paths) {
			
			Path least_cost_path = paths.getFirst();
			List<Path> alternatives = paths.getSecond();
			
			Double length_alt = getPathLength(alternative);
			
			List PS_bb_alt = new ArrayList();
			
			//in this case, the similarity factor counts the number of links a given route shares
			//with other routes in the choice set and weights it as one over the length of the respective route
			for (Link l : alternative.links) {
				Double similarity_alt = 0.0;
				
				if (least_cost_path.links.contains(l)) {
					similarity_alt += 1/getPathLength(least_cost_path);
				}
				for (Path path_alt : alternatives) {
					if (path_alt.links.contains(l)) {
						similarity_alt += 1/getPathLength(path_alt);
					}
				}
				//the path size is the sum of terms of the following form
				PS_bb_alt.add(l.getLength()/(getPathLength(least_cost_path) * similarity_alt));
			}
			
			Double bb_path_size_alt = 1/length_alt * PS_bb_alt.stream().mapToDouble(f -> ((Double) f).doubleValue()).sum();
			
			
			return bb_path_size_alt;
			
		}
		
		/**
		 * @param alternative
		 *            Any particular route (includes alternative = chosen route) in the choice set.
		 *
		 * @param paths
		 *            Set of alternatives: a tuple consisting of the least cost path, and a list of other alternatives.
		 * @param gamma
		 *            Parameter greater or equal to zero.
		 * @return path_size_alt
		 *            Returns the generalised path size of the selected route with respect to the given set of alternatives and a chosen route.
		 */
		
		public final Double getGeneralizedPathSize(Path alternative, Tuple<Path, List<Path>> paths, Double phi) {
			
			Path least_cost_path = paths.getFirst();
			List<Path> alternatives = paths.getSecond();
			
			Double length_alt = getPathLength(alternative);
			
			List PS_gen_alt = new ArrayList();
			
			//in this case, the similarity factor counts the number of links a given route shares
			//with other routes in the choice set and weights it as the ratio of the length of the considered alternative 
			//and of the overlapping route to the power of phi
			for (Link l : alternative.links) {
				Double similarity_alt = 0.0;
			
				if (least_cost_path.links.contains(l)) {
					similarity_alt += Math.pow(length_alt/getPathLength(least_cost_path), phi);
				}
				for (Path path_alt : alternatives) {
					if (path_alt.links.contains(l)) {
						similarity_alt += Math.pow(length_alt/getPathLength(path_alt), phi);
					}
				}
				//the path size is the sum of terms of the following form
				PS_gen_alt.add(l.getLength()/similarity_alt);
			}
			
			Double gen_path_size_alt = 1/length_alt * PS_gen_alt.stream().mapToDouble(f -> ((Double) f).doubleValue()).sum();
			
			
			return gen_path_size_alt;
			
		}
		
		
		
		/**
		 * @param alternative
		 *            Any particular route (includes alternative = chosen route) in the choice set.
		 *
		 * @param paths
		 *            Set of alternatives: a tuple consisting of the least cost path, and a list of other alternatives.
		 * @return path_size_cr
		 *            Returns the path size of the selected route with respect to the given set of alternatives and a chosen route.
		 */
		
		public final Double getPathSize(Path alternative, Tuple<Path, List<Path>> paths) {
			
			Path least_cost_path = paths.getFirst();
			List<Path> alternatives = paths.getSecond();
			
			Double length_alt = getPathLength(alternative);
			
			List PS_alt = new ArrayList();
			
			//the similarity factor counts the number of links a given route shares
			//with other routes in the choice set
			for (Link l : alternative.links) {
				int similarity_alt = 0;
				
				if (least_cost_path.links.contains(l)) {
					similarity_alt ++;
				}
				for (Path path_alt : alternatives) {
					if (path_alt.links.contains(l)) {
						similarity_alt ++;
					}
				}
				//the path size is the sum of terms of the following form
				PS_alt.add(l.getLength()/similarity_alt);
			}
			
			Double path_size_alt = 1/length_alt * PS_alt.stream().mapToDouble(f -> ((Double) f).doubleValue()).sum();
			
			
			return path_size_alt;
			
		}
		
		/**
		 * @param alternative
		 *            Any particular route (includes alternative = chosen route) in the choice set.
		 *
		 * @param paths
		 *            Set of alternatives: a tuple consisting of the least cost path, and a list of other alternatives.
		 * @return path_size_cr
		 *            Returns the path size correction term of Bovy et al of the selected route with respect to the given set of alternatives and a chosen route.
		 */
		
		public final Double getPathSizeCorrection(Path alternative, Tuple<Path, List<Path>> paths) {
			
			Path least_cost_path = paths.getFirst();
			List<Path> alternatives = paths.getSecond();
			
			Double length_alt = getPathLength(alternative);
			
			List PSC_alt = new ArrayList();
			
			//the similarity factor counts the number of links a given route shares
			//with other routes in the choice set
			for (Link l : alternative.links) {
				int similarity_alt = 0;
				
				if (least_cost_path.links.contains(l)) {
					similarity_alt ++;
				}
				for (Path path_alt : alternatives) {
					if (path_alt.links.contains(l)) {
						similarity_alt ++;
					}
				}
				//the path size is the sum of terms of the following form
				PSC_alt.add(l.getLength() * Math.log(similarity_alt));
			}
			
			Double path_size_correction_alt = 1/length_alt * PSC_alt.stream().mapToDouble(f -> ((Double) f).doubleValue()).sum();
			
			
			return path_size_correction_alt;
			
		}

		
		/**
		 * @param paths
		 *            One particular alternative route.
		 *
		 * @param paths
		 *            Set of alternatives: a tuple consisting of the least cost path, and a list of other alternatives.
		 * @return
		 *            Returns the commonality factor of one alternative with respect to the given set of alternatives and a chosen route (this includes the case
		 *            where as alternative we choose the chosen route).
		 */
		
		public Double getCommonalityFactor(Path alternative, Tuple<Path, List<Path>> paths) {
			
			List CF_alt = new ArrayList();
			Path least_cost_path = paths.getFirst();
			List<Path> alternatives = paths.getSecond();
			
			Double length_lcp = getPathLength(least_cost_path);
			
			Double length_alt = getPathLength(alternative);
			
			for (Link l : alternative.links) {
				if (least_cost_path.links.contains(l)) {
					CF_alt.add(l.getLength() / Math.sqrt(length_lcp));
				}
				for (Path path_alt : alternatives) {
					if (path_alt.links.contains(l)) {
						CF_alt.add(l.getLength() / Math.sqrt(getPathLength(path_alt)));
					}
				}
			}
			
			Double commonality_factor_alt = Math.log(1/Math.sqrt(length_alt) * CF_alt.stream().mapToDouble(f -> ((Double) f).doubleValue()).sum());
			return commonality_factor_alt;
			
		}
}
