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

package org.nlpub.watset.wsi;

import org.jgrapht.Graph;
import org.nlpub.watset.graph.Clustering;
import org.nlpub.watset.util.Neighbors;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A simple graph-based word sense induction approach. It clusters node neighborhoods.
 *
 * @param <V> node class.
 * @param <E> edge class.
 * @see <a href="https://doi.org/10.3115/1067737.1067753">Dorow &amp; Widdows (EACL '03)</a>
 */
public class SenseInduction<V, E> {
    private final Graph<V, E> graph;
    private final Function<Graph<V, E>, Clustering<V>> local;

    /**
     * Constructs a sense inducer.
     *
     * @param graph an input graph.
     * @param local a neighborhood clustering local provider.
     */
    public SenseInduction(Graph<V, E> graph, Function<Graph<V, E>, Clustering<V>> local) {
        this.graph = requireNonNull(graph);
        this.local = requireNonNull(local);
    }

    /**
     * Gets the induced senses and their non-disambiguated contexts.
     *
     * @param target a target node.
     * @return a map of senses to their contexts.
     */
    public Map<Sense<V>, Map<V, Number>> induce(V target) {
        final Graph<V, E> ego = Neighbors.neighborhoodGraph(graph, requireNonNull(target));

        final Clustering<V> clustering = local.apply(ego);
        clustering.run();

        final Collection<Collection<V>> clusters = clustering.getClusters();

        final Map<Sense<V>, Map<V, Number>> senses = new HashMap<>();

        int i = 0;

        for (final Collection<V> cluster : clusters) {
            final Map<V, Number> context = new HashMap<>(cluster.size());

            for (final V neighbor : cluster) {
                context.put(neighbor, graph.getEdgeWeight(graph.getEdge(target, neighbor)));
            }

            senses.put(new IndexedSense<>(target, i++), context);
        }

        return senses;
    }
}
