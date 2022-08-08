# MATSimBFSLE
A MATSim compatible implementation of the BFS-LE choice set generation algorithm.

The Breadth First Search on Link Elimination (BFS-LE) algorithm is a route choice set generation algorithm developed by Michael Balmer. The main reference is

Kay W. Axhausen, Nadine Rieser-Schüssler, and Michael Balmer. 
“Route choice sets for very high-resolution data”. en. Transportmetrica A: Transport Science 9.9 (2013), pp. 825 –845

The original code is due to Michael Balmer, now at Senozon AG.

Additionally, the code is extended to include a link penalty variant of the BFS-LE. Since the BFS-LE algorithm consists of repeated least-cost path computations, the link penalty variant enables the analyst to penalize links (i.e. increase their travel cost) that are part of the alternatives that make up the choice set up to that point.

The code contains analysis scripts for two simple examples - one uses the original BFS-LE algorithm, the other its link penalty variant - to get accustomed with the algorithms. 

The other two examples of the link penalty variant are specific to an IVT-ETH project, and are for internal-use only. The reference here is

Meister, Adrian, Felder, Matteo, Schmid, Basil and Kay W. Axhausen. 2022.
"Route choice modelling for cyclists on dense urban networks". Arbeitsberichte Verkehrs- und Raumplanung 1764.
