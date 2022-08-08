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

package org.ivt.linkpenaltybfsle;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.ivt.tools.ParseInputFile;
import org.ivt.tools.Triple;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class RunAnalysisLPBFSLE {
	
	private static final Logger log = Logger.getLogger(RunAnalysisLPBFSLE.class);
	
	/**
	 * generates choice sets, analyses routes according to the AnalysisLPBFSLE file.
	 * @param args                 nofPaths:          the number of paths generated per od pair (int >= 0)
	 *                             variantionFactor:  degree of variation in the generated path set (double >= 1.0)
	 *                             timeout:           maximum calc time of one OD pair in milliseconds (1000 <= long <= 604800000) [1 sec..1 week]
	 *                             inputNetworkFile:  matsim input XML network file (String)
	 *                             inputODFile:       input id|origin|destination|link_ids tab seperated table (String)
	 *                             outputPathSetFile: output path set file (String)
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 6) {
			log.error("Usage: RunAnalysisLPBFSLEGreaterZurich nofPaths variantionFactor timeout inputNetworkFile inputODFile outputPathSetFile");
			log.error("       nofPaths:          the number of paths generated per od pair (int >= 0)");
			log.error("       variantionFactor:  degree of variation in the generated path set (double >= 1.0)");
			log.error("       timeout:           maximum calc time of one OD pair in milliseconds (1000 <= long <= 604800000) [1 sec..1 week]");
			log.error("       inputNetworkFile:  matsim input XML network file (String)");
			log.error("       inputODFile:       input id|origin|destination|link_ids tab seperated table (String)");
			log.error("       outputPathSetFile: output path set file (String)");
			log.error("----------------");
			log.error("2009, matsim.org");
			throw new RuntimeException("incorrect number of arguments");
		}
		
		Gbl.printSystemInfo();

		long startTimeMilliseconds = System.currentTimeMillis();
		log.warn("The start time is now: " + startTimeMilliseconds);
		
		
		// get the arguments
		
		int nofPaths = Integer.parseInt(args[0]);
		double variationFactor = Double.parseDouble(args[1]);
		long timeout = Long.parseLong(args[2]);
		String inputNetworkFile = args[3];
		String inputODFile = args[4];
		String outputPathSetFile = args[5];

		log.info("nofPaths:          "+nofPaths);
		log.info("variationFactor:  "+variationFactor);
		log.info("timeout:           "+timeout);
		log.info("inputNetworkFile:  "+inputNetworkFile);
		log.info("inputODFile:       "+inputODFile);
		log.info("outputPathSetFile: "+outputPathSetFile);
		
		
		// get the network
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(inputNetworkFile);
		
		// read the input file and convert it to the correct format
		
		Map<String,Triple<Node,Node,Path>> ods_chosenRoutes = ParseInputFile.parseODandChosenRoutes(inputODFile,network);
		
		// call the analysis file which writes to the output file
		
		AnalysisLPBFSLE.analysis(outputPathSetFile, network, ods_chosenRoutes, nofPaths, variationFactor, nofPaths);
		
		log.warn("The time now is: " + System.currentTimeMillis());
		long calcTime = System.currentTimeMillis()-startTimeMilliseconds;
		
		
		log.warn("It took us " + calcTime + " ms to compute everything.");
	}
}


