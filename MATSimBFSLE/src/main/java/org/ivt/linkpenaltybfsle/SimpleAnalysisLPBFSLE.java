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



package org.ivt.linkpenaltybfsle;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.ivt.tools.AnalysisTools;
import org.ivt.tools.Triple;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.Tuple;

public class SimpleAnalysisLPBFSLE {
	
	//variables
	
	private static final Logger log = Logger.getLogger(SimpleAnalysisLPBFSLE.class);
	
	//methods
	/**
	 * generates alternatives using LPBFSLE algorithm and writes the specified attributes to a csv file
	 * in a biogeme compatible format.
	 * 
	 * @param outputFileName               csv file to write analysis
	 * @param network                      MATSim network
	 * @param ods_chosenRoutes             triple consisting of origin, destination, chosen route
	 * @param choice_set_size              number of routes to be generated
	 * @param variationFactor              variation factor
	 * @param timeout                      max computation time
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	
	
	public static final void analysis(String outputFileName, Network network, Map<String,Triple<Node,Node,Path>> ods_chosenRoutes, int choice_set_size, double variationFactor, long timeout) throws IOException, NumberFormatException {
		FileWriter fw = new FileWriter(outputFileName);
		BufferedWriter out = new BufferedWriter(fw);
		
		LinkPenaltyCostFunction cost_function = new LinkPenaltyCostFunction();
		
		Map<String,Double> used_links = new HashMap<>();
		
		for (Link l : network.getLinks().values()) {
			used_links.put(l.getId().toString(), 0.0);
		}
		
		cost_function.setUsedLinks(used_links);
		
		LPBFSLE bfsle = new LPBFSLE(network);
					
		bfsle.setChoiceSetSize(choice_set_size);
		bfsle.setVariationFactor(variationFactor);
		bfsle.setTimeout(timeout);
		
		// header of csv file
		
		out.write("OD_pair_id" + ",");
		out.write("computation_time" + ",");
		
		
		for (int i = 1; i <= choice_set_size; i++){
			out.write("length_" + String.valueOf(i) +  ",");
			out.write("number_of_links_" + String.valueOf(i) + ",");
			}
		//for biogeme path size logit we need to specify which route is the chosen one
		out.write("CHOICE" + "\n");
		
		int skip = 0;
		
		for (Entry<String, Triple<Node, Node, Path>> entry : ods_chosenRoutes.entrySet()) {
			long startTime = System.currentTimeMillis();
			String id = entry.getKey();
			
			skip ++;
			
			// if you only want to analyse a subset of your sample, this can be imposed here.
			if (skip < 0) {
				continue;
			}
			
			if (skip > 5000) {
				break;
			}
			
			
			Triple<Node, Node, Path> od_cr = entry.getValue();
			// generate paths

			log.debug("----------------------------------------------------------------------");
			log.debug("generating path sets for segment id="+id+", O="+od_cr.getFirst().getId()+" and D="+od_cr.getSecond().getId()+"...");
			if (bfsle.setODPair(od_cr.getFirst(),od_cr.getSecond()))
				try {
					
						Tuple<Path,List<Path>> paths = bfsle.generateChoiceSet(od_cr.getFirst(), od_cr.getSecond(), choice_set_size);
						log.debug("done.");
						long endTime = System.currentTimeMillis();
						long calcTime = endTime - startTime;
						
						
						// od pair id
						
						out.write(id + ",");
						
						// computation time
						
						out.write(calcTime + ",");
						
						// the chosen route
						
						Path chosen_route = od_cr.getThird();
						
						// the least cost path
						
						Path least_cost_path = paths.getFirst();
						
						// list of other alternatives
						
						List<Path> alternatives = paths.getSecond();
						
						Map <Integer, Path> internal_ids = new HashMap<>();
						
						internal_ids.put(0, chosen_route);
						internal_ids.put(1, least_cost_path);
						
						int j = 2;
						for (Path alternative : alternatives) {
							internal_ids.put(j, alternative);
							j ++;
						}
						
						//initialise analysis tools
						
						AnalysisTools analysis_tools = new AnalysisTools(chosen_route, paths);
						
///////////////////////// START ANALYSIS ///////////////////////////////
						
						for (Map.Entry<Integer, Path> choice_set_entry : internal_ids.entrySet()) {
							Path path = choice_set_entry.getValue();
							
							// length
							
							Double length = analysis_tools.getPathLength(path);
							
							out.write(length + ",");
							
							// number of links
							
							int number_of_links = path.links.size();
							
							out.write(number_of_links + ",");
							
						}
				out.write("1" + "\n");
				
					
				} catch (NumberFormatException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			else {
				log.warn("triple id="+id+", O="+od_cr.getFirst().getId()+" and D="+od_cr.getSecond().getId()+" is omitted.");
			}
			Gbl.printMemoryUsage();
			log.debug("----------------------------------------------------------------------");
		}
		//out.append('\n');
		out.close();
		fw.close();
		
	}

}


