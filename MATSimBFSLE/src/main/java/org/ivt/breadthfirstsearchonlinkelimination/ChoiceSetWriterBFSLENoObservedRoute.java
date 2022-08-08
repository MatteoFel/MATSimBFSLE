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
@author      Michael Balmer, Senozon AG
@author      Matteo Felder
*/

package org.ivt.breadthfirstsearchonlinkelimination;

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
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.Tuple;

public class ChoiceSetWriterBFSLENoObservedRoute {
	
	//variables
	
	private static final Logger log = Logger.getLogger(ChoiceSetWriterBFSLENoObservedRoute.class);
	
	//methods
	
	public static final void choiceSetWriter(String outputFileName, Network network, Map<String,Tuple<Node,Node>> ods, int choice_set_size, double variationFactor, long timeout) throws IOException {
		FileWriter fw = new FileWriter(outputFileName);
		BufferedWriter out = new BufferedWriter(fw);
		
		DistanceCostFunction cost_function = new DistanceCostFunction();
		
		GlobalConfigGroup globalConfigGroup = new GlobalConfigGroup();
		
		LeastCostPathCalculator router = new AStarLandmarksFactory(globalConfigGroup).createPathCalculator(network, cost_function, cost_function);
		
		
		BFSLE bfsle = new BFSLE(network, cost_function, router);
					
		bfsle.setChoiceSetSize(choice_set_size);
		bfsle.setVariationFactor(variationFactor);
		bfsle.setTimeout(timeout);
		
		
		// csv file header
		
		out.write("od_pair_id" + ",");
		out.write("internal_id" + ",");
		out.write("link_nodetonode_id" + ",");
		out.append('\n');
		
		int skip = 0;

		for (Entry<String, Tuple<Node, Node>> entry : ods.entrySet()) {

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
				
			
			Tuple<Node, Node> od = entry.getValue();
			// generate paths

			log.debug("----------------------------------------------------------------------");
			log.debug("generating path sets for segment id="+id+", O="+od.getFirst().getId()+" and D="+od.getSecond().getId()+"...");
			if (bfsle.setODPair(od.getFirst(),od.getSecond()))
				try {
					Tuple<Path,List<Path>> paths = bfsle.generateChoiceSet(od.getFirst(), od.getSecond(), choice_set_size);
					log.debug("done.");
					long endTime = System.currentTimeMillis();
					long calcTime = endTime - startTime;
					
					
					// the least cost path
					
					Path least_cost_path = paths.getFirst();
					
					// list of other alternatives
					
					List<Path> alternatives = paths.getSecond();
					
					// set up internal id's of choice set
					
					Map <Integer, Path> internal_ids = new HashMap<>();

					internal_ids.put(0, least_cost_path);
					
					int j = 1;
					for (Path alternative : alternatives) {
						internal_ids.put(j, alternative);
						j ++;
					}
					
					
					
					
					for (Map.Entry<Integer, Path> choice_set_entry : internal_ids.entrySet()) {
						int internal_id = choice_set_entry.getKey();
						Path path = choice_set_entry.getValue();
						
						out.write(id + ",");
						out.write(internal_id + ",");
						
						//write out the route links are represent by node-to-node ids
						for (Link l : path.links) {
							out.write(String.valueOf(l.getId()) + ",");
							}
					
						out.append('\n');
						
					}
					
				} catch (NumberFormatException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			else {
				log.warn("triple id="+id+", O="+od.getFirst().getId()+" and D="+od.getSecond().getId()+" is omitted.");
			}
			Gbl.printMemoryUsage();
			log.debug("----------------------------------------------------------------------");
		}
		out.close();
		fw.close();
		}

}





