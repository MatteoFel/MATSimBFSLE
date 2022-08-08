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


package org.ivt.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.Tuple;

public class ParseInputFile {
	
	private static final Logger log = Logger.getLogger(ParseInputFile.class);

	public static final Map<String, Tuple<Node,Node>> parseODs(String inputFileName, Network network) throws IOException {
		Map<String, Tuple<Node,Node>> ods = new TreeMap<>();
		int lineCnt = 0;
		FileReader fr = new FileReader(inputFileName);
		BufferedReader in = new BufferedReader(fr);

		// Skip header
		String currLine = in.readLine(); lineCnt++;
		while ((currLine = in.readLine()) != null) {
			String[] entries = currLine.split("\t", -1);
			// IDSEGMENT Origin Destination 
			// 0          1         2		
			
			String id = entries[0].trim();
			Node origin = network.getNodes().get(Id.create(entries[1].trim(), Node.class));
			Node destination = network.getNodes().get(Id.create(entries[2].trim(), Node.class));
			if ((origin == null) || (destination == null)) 
			{log.warn("line " +lineCnt+ " id " + id + ": O and/or D not found in the network");}//{ throw new RuntimeException("line " +lineCnt+ " id " + id + ": O and/or D not found in the network"); }
			
			ods.put(id,new Tuple<Node,Node>(origin,destination));
			// progress report
			if (lineCnt % 100000 == 0) { log.debug("line "+lineCnt); }
			lineCnt++;
		}
		in.close();
		fr.close();
		log.debug("# lines read: " + lineCnt);
		log.debug("# OD pairs: " + ods.size());
		return ods;
	}
	
	
	public static final Map<String, Triple<Node,Node,Path>> parseODandChosenRoutes(String inputFileName, Network network) throws IOException {
		Map<String, Triple<Node,Node,Path>> ods_chosenRoutes = new TreeMap<>();
		int lineCnt = 0;
		FileReader fr = new FileReader(inputFileName);
		BufferedReader in = new BufferedReader(fr);

		// Skip header
		String currLine = in.readLine(); lineCnt++;
		while ((currLine = in.readLine()) != null) {
			String[] entries = currLine.split("\t", -1);
			// IDSEGMENT Origin Destination LinkId1  LinkId2 ... LinkId
			// 0          1         2		 3	        4         last
			
			
			String id = entries[0].trim();
			Node origin = network.getNodes().get(Id.create(entries[1].trim(), Node.class));
			Node destination = network.getNodes().get(Id.create(entries[2].trim(), Node.class));
			if ((origin == null) || (destination == null)) 
			{log.warn("line " +lineCnt+ " id " + id + ": O and/or D not found in the network");}//{ throw new RuntimeException("line " +lineCnt+ " id " + id + ": O and/or D not found in the network"); }
			
			
			List<Node> nodes = new ArrayList<Node>();
			List<Link> links = new ArrayList<Link>();
			for (int i = 3; i < entries.length; i++) {
				if (entries[i].trim().length()==0) {
					break;
				}
				Link link = network.getLinks().get(Id.create(entries[i].trim(), Link.class));
				if (link == null)
				{log.warn("od pair " + id + " and i is " + i + " and link with id " + entries[i].trim() + " does not appear in the network.");}
				links.add(link);
				Node fromNode = link.getFromNode();
				nodes.add(fromNode);
			}
			Link lastLink = links.get(links.size() - 1);
			nodes.add(lastLink.getToNode());
			Path path = new Path(nodes, links, 0.0, 0.0);
			
			ods_chosenRoutes.put(id,new Triple<Node,Node,Path>(origin,destination,path));
			// progress report
			if (lineCnt % 100000 == 0) { log.debug("line "+lineCnt); }
			lineCnt++;
		}
		in.close();
		fr.close();
		log.debug("# lines read: " + lineCnt);
		log.debug("# OD pairs: " + ods_chosenRoutes.size());
		return ods_chosenRoutes;
	}
}
