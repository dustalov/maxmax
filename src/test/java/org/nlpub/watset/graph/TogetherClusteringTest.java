/*
 * Copyright 2018 Dmitry Ustalov
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

package org.nlpub.watset.graph;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TogetherClusteringTest {
    private final TogetherClustering<String, DefaultWeightedEdge> together = TogetherClustering.<String, DefaultWeightedEdge>builder().apply(Fixtures.TWO_COMPONENTS);

    @Test
    public void testClustering() {
        final var clustering = together.getClustering();
        assertEquals(1, clustering.getNumberClusters());
        assertEquals(Fixtures.TWO_COMPONENTS.vertexSet(), clustering.getClusters().toArray()[0]);
    }
}
