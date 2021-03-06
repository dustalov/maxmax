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

package org.nlpub.watset.graph;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.ClusteringAlgorithm;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static org.jgrapht.GraphTests.requireUndirected;
import static org.nlpub.watset.util.Maximizer.argrandmax;

/**
 * Implementation of the Chinese Whispers algorithm.
 *
 * @param <V> the type of nodes in the graph
 * @param <E> the type of edges in the graph
 * @see <a href="https://doi.org/10.3115/1654758.1654774">Biemann (TextGraphs-1)</a>
 */
public class ChineseWhispers<V, E> implements ClusteringAlgorithm<V> {
    /**
     * Builder for {@link ChineseWhispers}.
     *
     * @param <V> the type of nodes in the graph
     * @param <E> the type of edges in the graph
     */
    @SuppressWarnings("unused")
    public static class Builder<V, E> implements ClusteringAlgorithmBuilder<V, E, ChineseWhispers<V, E>> {
        /**
         * The default number of Chinese Whispers iterations.
         */
        public static final int ITERATIONS = 20;

        private NodeWeighting<V, E> weighting;
        private int iterations;
        private Random random;

        /**
         * Create an instance of {@link ChineseWhispers} builder.
         */
        public Builder() {
            this.weighting = NodeWeightings.top();
            this.iterations = ITERATIONS;
            this.random = new Random();
        }

        @Override
        public ChineseWhispers<V, E> apply(Graph<V, E> graph) {
            return new ChineseWhispers<>(graph, weighting, iterations, random);
        }

        /**
         * Set the the node weighting approach.
         *
         * @param weighting the node weighting approach
         * @return the builder
         */
        public Builder<V, E> setWeighting(NodeWeighting<V, E> weighting) {
            this.weighting = requireNonNull(weighting);
            return this;
        }

        /**
         * Set the maximal number of iterations.
         *
         * @param iterations the maximal number of iterations
         * @return the builder
         */
        public Builder<V, E> setIterations(int iterations) {
            this.iterations = iterations;
            return this;
        }

        /**
         * Set the random number generator.
         *
         * @param random the random number generator
         * @return the builder
         */
        public Builder<V, E> setRandom(Random random) {
            this.random = requireNonNull(random);
            return this;
        }
    }

    /**
     * Create a builder.
     *
     * @param <V> the type of nodes in the graph
     * @param <E> the type of edges in the graph
     * @return a builder
     */
    public static <V, E> Builder<V, E> builder() {
        return new Builder<>();
    }

    /**
     * The graph.
     */
    protected final Graph<V, E> graph;

    /**
     * The node weighting approach.
     */
    protected final NodeWeighting<V, E> weighting;

    /**
     * The number of iterations.
     */
    protected final int iterations;

    /**
     * The random number generator.
     */
    protected final Random random;

    /**
     * The cached clustering result.
     */
    protected Clustering<V> clustering;

    /**
     * Create an instance of the Chinese Whispers algorithm.
     *
     * @param graph      the graph
     * @param weighting  the node weighting approach
     * @param iterations the number of iterations
     * @param random     the random number generator
     */
    public ChineseWhispers(Graph<V, E> graph, NodeWeighting<V, E> weighting, int iterations, Random random) {
        this.graph = requireUndirected(graph);
        this.weighting = requireNonNull(weighting);
        this.iterations = iterations;
        this.random = requireNonNull(random);
    }

    @Override
    public Clustering<V> getClustering() {
        if (isNull(clustering)) {
            clustering = new Implementation<>(graph, weighting, iterations, random).compute();
        }

        return clustering;
    }

    /**
     * Actual implementation of Chinese Whispers.
     *
     * @param <V> the type of nodes in the graph
     * @param <E> the type of edges in the graph
     */
    protected static class Implementation<V, E> {
        /**
         * The graph.
         */
        protected final Graph<V, E> graph;

        /**
         * The node weighting approach.
         */
        protected final NodeWeighting<V, E> weighting;

        /**
         * The number of iterations.
         */
        protected final int iterations;

        /**
         * The random number generator.
         */
        protected final Random random;

        /**
         * The mapping of nodes to labels.
         */
        protected final Map<V, Integer> labels;

        /**
         * The number of actual algorithm iterations.
         */
        protected int steps;

        /**
         * Create an instance of the Chinese Whispers clustering algorithm implementation.
         *
         * @param graph      the graph
         * @param weighting  the node weighting approach
         * @param iterations the number of iterations
         * @param random     the random number generator
         */
        public Implementation(Graph<V, E> graph, NodeWeighting<V, E> weighting, int iterations, Random random) {
            this.graph = graph;
            this.weighting = weighting;
            this.iterations = iterations;
            this.random = random;
            this.labels = new HashMap<>(graph.vertexSet().size());
        }

        /**
         * Perform clustering with Chinese Whispers.
         *
         * @return the clustering
         */
        public Clustering<V> compute() {
            final var nodes = new ArrayList<>(graph.vertexSet());

            var i = 0;

            for (final var node : graph.vertexSet()) {
                labels.put(node, i++);
            }

            for (steps = 0; steps < iterations; steps++) {
                Collections.shuffle(nodes, random);

                if (step(nodes) == 0) break;
            }

            final var groups = labels.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue));

            final List<Set<V>> clusters = new ArrayList<>(groups.size());

            for (final var cluster : groups.values()) {
                clusters.add(cluster.stream().map(Map.Entry::getKey).collect(Collectors.toSet()));
            }

            return new ClusteringImpl<>(clusters);
        }

        /**
         * Perform one iteration of the algorithm.
         *
         * @param nodes the list of nodes
         * @return whether any label changed or not
         */
        protected int step(List<V> nodes) {
            var changed = 0;

            for (final var node : nodes) {
                final var scores = score(node);

                final var label = argrandmax(scores.entrySet(), Map.Entry::getValue, random);

                final int updated = label.isPresent() ? label.get().getKey() : labels.get(node);

                // labels.put() never returns null for a known node
                @SuppressWarnings("ConstantConditions") final int previous = labels.put(node, updated);

                if (previous != updated) {
                    changed++;
                }
            }

            return changed;
        }

        /**
         * Score the label weights in the given neighborhood graph, which is a subgraph of {@link #graph}.
         * This method sums the node weights corresponding to each label.
         *
         * @param node the target node
         * @return a mapping of labels to sums of their weights
         */
        protected Map<Integer, Double> score(V node) {
            final var edges = graph.edgesOf(node);

            final var weights = new HashMap<Integer, Double>(edges.size());

            for (final var edge : edges) {
                final var neighbor = Graphs.getOppositeVertex(graph, edge, node);
                final int label = labels.get(neighbor);
                weights.merge(label, weighting.apply(graph, labels, node, neighbor), Double::sum);
            }

            return weights;
        }

        /**
         * Return the number of iterations specified in the constructor
         *
         * @return the number of iterations
         */
        @SuppressWarnings("unused")
        public int getIterations() {
            return iterations;
        }

        /**
         * Return the number of iterations actually performed during {@link #getClustering()}.
         * Should be no larger than the value of {@link #getIterations()}.
         *
         * @return the number of iterations
         */
        @SuppressWarnings("unused")
        public int getSteps() {
            return steps;
        }
    }
}
