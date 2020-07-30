/*
 * Copyright 2020 Dmitry Ustalov
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

package org.nlpub.watset.eval;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Before;
import org.junit.Test;
import org.nlpub.watset.graph.ChineseWhispersTest;

import static org.junit.Assert.assertEquals;

public class MeasurerTest {
    static final Measurer<String, DefaultWeightedEdge> MEASURER = new Measurer<>(ChineseWhispersTest.BUILDER.provider(), ChineseWhispersTest.DISJOINT);

    @Before
    public void setUp() {
        MEASURER.run();
    }

    @Test
    public void testGetGraph() {
        assertEquals(ChineseWhispersTest.DISJOINT, MEASURER.getGraph());
    }

    @Test
    public void testGetDurations() {
        assertEquals(Measurer.REPETITIONS, MEASURER.getDurations().size());
    }

    @Test
    public void testGetClusters() {
        assertEquals(Measurer.REPETITIONS, MEASURER.getClusters().size());
    }
}