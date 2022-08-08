/* *********************************************************************** *
 * project: org.matsim.*
 * BFSLE.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
*
* original version by Michael Balmer, adapted by Matteo Felder
*/

package org.ivt.breadthfirstsearchonlinkelimination;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import org.ivt.choicesetgenerator.ChoiceSetGenerator;
import org.ivt.tools.StreetSegment;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

public class BFSLE implements ChoiceSetGenerator {
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	
	private final static Logger log = Logger.getLogger(ChoiceSetGenerator.class);

	/**
	 * The network on which we find routes.
	 */
	protected Network network;

	/**
	 * The cost calculator. Provides the cost for each link.
	 */
	protected final TravelDisutility travelDisutility;
	
	/**
	 * The router with which we find least cost paths
	 */
	protected final LeastCostPathCalculator router;
	
	
	// this is not very nice...: keep the leastCostPath in mind (path on level zero) during the
	// getPaths() method.
	
	/**
	 * The cheapest route.
	 */
	private Path leastCostPath = null;	
	/**
	 * The origin.
	 */
	private Node origin = null;
	/**
	 * The destination.
	 */
	private Node destination = null;
	/**
	 * The number of alternatives to be generated.
	 */
	private int choiceSetSize = 20; // default
	/**
	 * The variation factor. TODO
	 */
	private double variationFactor = 1.0; // default
	
	/**
	 * The departure time. Not sure yet if needed. Setting default.
	 */
	private double depTime = Time.MIDNIGHT; // default
	/**
	 * Running variable.
	 */
	private int routeCnt = 0;
	/**
	 * Maximal computation time.
	 */
	private long timeout = 604800000; // default 1 week


	// this is also not very nice: to keep the start time for calc one path set
	/**
	 * Starting time. For computation time calculations.
	 */
	private long startTimeMilliseconds = System.currentTimeMillis();
	
	/**
	 * Link to street segment mapping in order to simplify the network.
	 */
	private final Map<Id<Link>,StreetSegment> l2sMapping = new HashMap<>();

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////
	/**
	 *
	 *
	 * @param network
	 *            The network on which to route.
	 * @param travelDisutility
	 *            Determines the link cost defining the cheapest route.
	 * @param router
	 *            Finds the cheapest route.
	 */
	
	public BFSLE(Network network, TravelDisutility travelDisutility, LeastCostPathCalculator router) {
		if (network == null) { throw new RuntimeException("Network must exist."); }
		
		this.network = network;
		this.travelDisutility = travelDisutility;
		this.router = router;
		
		// initialize the converting map from the original network to the topologically equivalent network.
		this.initStreetSegments();
		
	}
	
	//////////////////////////////////////////////////////////////////////
	// public methods
	//////////////////////////////////////////////////////////////////////

	public final void setChoiceSetSize(int choiceSetSize) {
		if (choiceSetSize < 1) { log.warn("Choice set size: "+choiceSetSize+" < 1 not allowed. Keeping previous choice set size = "+this.choiceSetSize); }
		else { this.choiceSetSize = choiceSetSize; }
	}

	public final void setVariationFactor(double variationFactor) {
		if (variationFactor < 1.0) { log.warn("variationFactor: "+variationFactor+" < 1.0 not allowed. Keeping previous variation factor: "+this.variationFactor); }
		else { this.variationFactor = variationFactor; }
	}

	public final void setTimeout(long timeout) {
		if ((timeout < 1000) || (timeout > (604800000))) { log.warn("timeout: "+timeout+" must be between 1 sec (1000 msec) and 7 days (604'800'000 msec). Keeping previous timeout: "+this.timeout); }
		else { this.timeout = timeout; }
	}

	public final boolean setODPair(Node fromNode, Node toNode) {
		if (fromNode == null) { log.warn("Origin node must exist."); return false; }
		if (network.getNodes().get(fromNode.getId()) == null) { log.warn("Origin node does not exist in the network."); return false; }

		if (toNode == null) { log.warn("Destination node must exist."); return false; }
		if (network.getNodes().get(toNode.getId()) == null) { log.warn("Destination node does not exist in the network."); return false; }

		if (fromNode.equals(toNode)) { log.warn("Origin equals to destination not allowed."); return false; }
		origin = fromNode;
		destination = toNode;
		return true;
	}
	
	public final void printL2SMapping() {
		for (Id<Link> id : l2sMapping.keySet()) {
			System.out.println(id.toString()+"\t"+l2sMapping.get(id).getId());
		}
	}

	
	/**
	 * In the case of BFSLE, this method first builds the recursion tree. Once it reaches the required number of alternatives, it still expands
	 * the level it is on, then stops. The set of alternatives might thus be too large. 
	 * In a second step, it randomly removes routes until the required choice set size is reached.
	 * 
	 * @param origin
	 *               the origin
	 * @param destination
	 *               the destination
	 * @param choiceSetSize
	 *               the number of routes to be generated
	 * @return 
	 *               A tuple consisting of the least cost path and a list containing all other alternatives
	 */
	 
	 
	@Override
	public final Tuple<Path, List<Path>> generateChoiceSet(Node origin, Node destination, int choiceSetSize) {
		
		// set calculation start time
		startTimeMilliseconds = System.currentTimeMillis();
		log.debug(" measurement started at "+startTimeMilliseconds+" with timeout "+timeout+"...");

		// setup and run the recursion
		List<Set<StreetSegment>> excludingStreetSegmentSets = new LinkedList<Set<StreetSegment>>();
		excludingStreetSegmentSets.add(new HashSet<StreetSegment>());
		Set<Path> paths = new HashSet<Path>();
		routeCnt = 0;
		buildRecursionTree(0,excludingStreetSegmentSets,paths);

		// remove the least cost path from the paths
		// this is not very nice... (see generate(...) why)
		paths.remove(leastCostPath);
		// remove randomly as many paths until choiceSetSize-1 are remaining
		List<Path> tmpPaths = new LinkedList<Path>(paths);
		while (tmpPaths.size() > (choiceSetSize-1)) { tmpPaths.remove(MatsimRandom.getRandom().nextInt(tmpPaths.size())); }
		// create the result containing the least cost path and choiceSetSize-1 other paths
		Tuple<Path,List<Path>> tuple = new Tuple<Path,List<Path>>(leastCostPath,tmpPaths);
		// reset the least cost path
		leastCostPath = null;
		return tuple;
		
	}
	
	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////
	
	
	/**
	 * Adds a link to the network.
	 * @param link
	 */
	private void addLinkToNetwork(Link link) {
		link.getFromNode().addOutLink(link);
		link.getToNode().addInLink(link);
	}
	
	/**
	 * Removes a link from the network.
	 * @param link
	 */
	private void removeLinkFromNetwork(Link link) {
		final Link outlink = link;
		final Id<Link> outLinkId = outlink.getId();
		((Node) link.getFromNode()).removeOutLink(outLinkId);
		final Link inlink = link;
		((Node) link.getToNode()).removeInLink(inlink.getId());
	}
	
	/**
	 * Returns true if a path is within a specified set of paths.
	 * @param paths
	 * @param path
	 * @return
	 */
	private final boolean containsPath(Set<Path> paths, Path path) {
		for (Path p : paths) {
			if (p.links.equals(path.links)) { return true; }
		}
		return false;
	}
	
	/**
	 * Returns true if a set of street segments is contained within a list of sets of street segments.
	 * @param streetSegmentSets
	 * @param streetSegmentSet
	 * @return
	 */
	private final boolean containsStreetSegmentIdSet(List<Set<StreetSegment>> streetSegmentSets, Set<StreetSegment> streetSegmentSet) {
		for (Set<StreetSegment> set : streetSegmentSets) {
			if (set.equals(streetSegmentSet)) { return true; }
		}
		return false;
	}
	
	/**
	 * This method simplifies the network. In particular it replaces links connecting nodes that do not model junctions, intersections or dead-ends (so called pass nodes)
	 * by a single link (called street segment) connecting two non-pass nodes. Notice that this replacement procedure is ONLY useful for the link elimination
	 * stage (and speeds up the algorithm), and NOT for the computation of the least cost path (where each individual link attributes plays a role).
	 */
	
	private final void initStreetSegments() {
		long timeBeforeInitStreetSegs = System.currentTimeMillis();
		log.warn("Before init street segments, the time is: " + System.currentTimeMillis());
		log.info("init street segments...");
		
		for (Link l : network.getLinks().values()) {

			// find the beginning of the "oneway path" or "twoway path"
			Link currLink = l;
			Node fromNode = (Node) currLink.getFromNode();
			Node toNodeLoopCheck = (Node) currLink.getToNode();
			

			while ((((Map<Id<Node>, ? extends Node>) NetworkUtils.getIncidentNodes(fromNode)).size() == 2) &&
					(((fromNode.getOutLinks().size() == 1) && (fromNode.getInLinks().size() == 1)) ||
					((fromNode.getOutLinks().size() == 2) && (fromNode.getInLinks().size() == 2)))) {
				toNodeLoopCheck = (Node) currLink.getToNode();
				if (fromNode == toNodeLoopCheck) {removeLinkFromNetwork(currLink); }
				Iterator<? extends Link> linkIt = fromNode.getInLinks().values().iterator();
				Link prevLink = linkIt.next();
				if (prevLink.getFromNode().getId().equals(currLink.getToNode().getId())) { prevLink = linkIt.next(); }
				currLink = prevLink;
				fromNode = (Node) currLink.getFromNode();
				toNodeLoopCheck = (Node) currLink.getToNode();
			}

			// create the street segment for the whole "one- or twoway path" (if not already exists)
			StreetSegment s = l2sMapping.get(currLink.getId());
			if (s == null) {
				s = new StreetSegment(Id.create("s"+currLink.getId(), Link.class),currLink.getFromNode(),currLink.getToNode(),network,1,1,1,1);
				s.links.add(currLink);
				l2sMapping.put(currLink.getId(),s);
				Node toNode = (Node) currLink.getToNode();
				while ((((Map<Id<Node>, ? extends Node>) NetworkUtils.getIncidentNodes(toNode)).size() == 2) &&
						(((toNode.getOutLinks().size() == 1) && (toNode.getInLinks().size() == 1)) ||
						((toNode.getOutLinks().size() == 2) && (toNode.getInLinks().size() == 2)))) {
					Iterator<? extends Link> linkIt = toNode.getOutLinks().values().iterator();
					Link nextLink = linkIt.next();
					if (nextLink.getToNode().getId().equals(currLink.getFromNode().getId())) { nextLink = linkIt.next(); }
					currLink = nextLink;
					toNode = (Node) currLink.getToNode();
					s.links.add(currLink);
					l2sMapping.put(currLink.getId(),s);
					s.setToNode(toNode);
				}
			}
		}
		
		
//		log.info("  Number of links in the network:         "+network.getLinks().size());
//		log.info("  Number of links in the mapping:         "+l2sMapping.size());
//		log.info("  Number of street segments: "+streetSegmentCnt);
//		Set<StreetSegment> segments = new HashSet<StreetSegment>(l2sMapping.values());
//	log.info("  Number of street segments:              "+segments.size());
//		int lcnt = 0;
//		for (StreetSegment s : segments) { for (Link l : s.links) { lcnt++; } }
//		log.info("  Number of links in the street segments: "+lcnt);
//		log.info("done.");
//		log.warn("After init street segments, the time is: " + System.currentTimeMillis());
		
		long calcTimeInitStreetSegs = System.currentTimeMillis()-timeBeforeInitStreetSegs;
		log.warn("It took us " + calcTimeInitStreetSegs + " ms to init street segs.");
	}
	
	
	/**
	 * Builds the (entire) recursion tree using breadth first search, and saves all generated paths as a set.
	 * @param level
	 *             The current level of the recursion tree.
	 * @param excludingStreetSegmentSets
	 *             The set of street segments to be delete in order to expand the current level.
	 * @param paths
	 *             The choice set generated up to this level.
	 */
	
	private final void buildRecursionTree(int level, List<Set<StreetSegment>> excludingStreetSegmentSets, Set<Path> paths) {
		log.debug("start level "+level);

		// for EARLY ABORT: shuffle the excludingLinkSets
		Collections.shuffle(excludingStreetSegmentSets,MatsimRandom.getRandom());

		// the set of excluding link sets for the NEXT tree level
		List<Set<StreetSegment>> newExcludingStreetSegmentSets = new LinkedList<Set<StreetSegment>>();

		// go through all given link sets for THIS level
		int setCnt = 0;
		for (Set<StreetSegment> streetSegmentSet : excludingStreetSegmentSets) {
			setCnt++;

			// remove the links from the network, calculate the least cost path and put the links back where they were
			for (StreetSegment segment : streetSegmentSet) {
				for (Link l : segment.links) {
					removeLinkFromNetwork(l);
				}
			}
			Path path = router.calcLeastCostPath(origin,destination,depTime, null, null);
			routeCnt++;
			for (StreetSegment segment : streetSegmentSet) {
				for (Link l : segment.links) {
					addLinkToNetwork(l);
				}
			}

			// check if there is a path from O to D (if not, that part of the recursion tree does not have to be expanded)
			if (path != null) {

				// add path to the path set (if not yet exists)
				if (!containsPath(paths,path)) {
					paths.add(path);
					log.debug("  path added (nofPath="+paths.size()+"; nofRemainingSets="+(excludingStreetSegmentSets.size()-setCnt)+")");
				}

				// this is not very nice...: keep the leastCostPath in mind (path on level zero)
				if (level == 0) { leastCostPath = path; }

				// EARLY ABORT: if the excludingLinkSets are shuffled already, there is no
				// need to go through the whole level anymore. Therefore,
				// if the number of paths is already enough, stop the process right here
				if (paths.size() >= (choiceSetSize*variationFactor)) {
					log.debug("  number of paths("+paths.size()+") >= choiceSetSize("+choiceSetSize+") * variationFactor("+variationFactor+")");
					log.debug("  ==> found enough paths from node "+origin.getId()+" to node "+destination.getId()+".");
					log.debug("end level "+level);
					//printSummary(origin, destination, System.currentTimeMillis()-startTimeMilliseconds, paths.size(), routeCnt, level,"OK", leastCostPath);
					return;
				}

				// TIMEOUT ABORT
				if (System.currentTimeMillis() > (startTimeMilliseconds+timeout)) {
					log.debug("  number of paths("+paths.size()+") < choiceSetSize("+choiceSetSize+") * variationFactor("+variationFactor+")");
					log.debug("  ==> calculation timeout ("+timeout+" msec) reached for from node "+origin.getId()+" to node "+destination.getId()+".");
					log.debug("end level "+level);
					//printSummary(origin, destination, System.currentTimeMillis()-startTimeMilliseconds, paths.size(), routeCnt, level, "TIMEOUT", leastCostPath);
					return;
				}

				// no matter if the path already exists in the path list, that element of the recursion tree needs to be expanded.
				// Therefore, add new excluding link set for the NEXT tree level
				// TODO balmermi: set the right street segments
				for (Link l : path.links) {
					Set<StreetSegment> newExcludingStreetSegmentSet = new HashSet<StreetSegment>(streetSegmentSet.size()+1);
					newExcludingStreetSegmentSet.addAll(streetSegmentSet);
					StreetSegment s = l2sMapping.get(l.getId());
					if (s == null) { log.fatal("THIS MUST NOT HAPPEN (linkid="+l.getId()+")"); }
					newExcludingStreetSegmentSet.add(l2sMapping.get(l.getId()));
					if (!containsStreetSegmentIdSet(newExcludingStreetSegmentSets,newExcludingStreetSegmentSet)) {
						newExcludingStreetSegmentSets.add(newExcludingStreetSegmentSet);
					}

//					Set<Link> newExcludingLinkSet = new HashSet<Link>(linkSet.size()+1);
//					newExcludingLinkSet.addAll(linkSet);
//					newExcludingLinkSet.add(l);
//					if (!containsLinkIdSet(newExcludingLinkSets,newExcludingLinkSet)) {
//						newExcludingLinkSets.add(newExcludingLinkSet);
//					}
				}
			}
		}

		// nothing more to expand and therefore, no next tree level
		if (newExcludingStreetSegmentSets.isEmpty()) {
			log.debug("  number of paths("+paths.size()+") < choiceSetSize("+choiceSetSize+") * variationFactor("+variationFactor+")");
			log.debug("  ==> there are no more paths from node "+origin.getId()+" to node "+destination.getId()+".");
			log.debug("end level "+level);
		//	printSummary(origin, destination, System.currentTimeMillis()-startTimeMilliseconds, paths.size(), routeCnt, level, "NOMOREPATH", leastCostPath);
		}
		// not enough paths found yet and therefore go into the next tree level
		else {
			log.debug("  newExcludingLinkIdSets.size() = "+newExcludingStreetSegmentSets.size());
			log.debug("  paths.size()                  = "+paths.size());
			log.debug("end level "+level);
			level++;
			buildRecursionTree(level,newExcludingStreetSegmentSets,paths);
		}
	}

}
