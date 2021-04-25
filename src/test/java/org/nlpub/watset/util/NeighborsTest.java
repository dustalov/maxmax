/*
 * Copyright 2019 Dmitry Ustalov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.nlpub.watset.util;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Test;
import org.nlpub.watset.graph.Fixtures;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class NeighborsTest {
    private final static Set<String> NEIGHBORHOOD = Graphs.neighborSetOf(Fixtures.WORD_GRAPH, "bank");
    private final static Graph<String, DefaultWeightedEdge> EGO_SUBGRAPH = new AsSubgraph<>(Fixtures.WORD_GRAPH, new HashSet<>(NEIGHBORHOOD));

    @Test
    public void testGraph() {
        final var ego = Neighbors.graph(Fixtures.WORD_GRAPH, "bank");
        assertEquals(EGO_SUBGRAPH.vertexSet(), ego.vertexSet());
        assertEquals(EGO_SUBGRAPH.edgeSet(), ego.edgeSet());
        assertFalse(ego.containsVertex("bank"));
    }
}
