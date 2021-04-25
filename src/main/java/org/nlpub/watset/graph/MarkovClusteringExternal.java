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
import org.jgrapht.util.VertexToIntegerMapping;

import java.io.*;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static org.jgrapht.GraphTests.requireUndirected;

/**
 * A wrapper for the official implementation of the Markov Clustering (MCL) algorithm in C.
 * <p>
 * This is a weird thing. The official implementation of MCL is very fast, but we need to run
 * the separate process and speak to it over standard input/output redirection.
 *
 * @param <V> the type of nodes in the graph
 * @param <E> the type of edges in the graph
 * @see <a href="https://hdl.handle.net/1874/848">van Dongen (2000)</a>
 * @see <a href="https://doi.org/10.1137/040608635">van Dongen (2008)</a>
 * @see <a href="https://micans.org/mcl/">MCL - a cluster algorithm for graphs</a>
 */
public class MarkovClusteringExternal<V, E> implements ClusteringAlgorithm<V> {
    /**
     * Builder for {@link MarkovClusteringExternal}.
     *
     * @param <V> the type of nodes in the graph
     * @param <E> the type of edges in the graph
     */
    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public static class Builder<V, E> implements ClusteringAlgorithmBuilder<V, E, MarkovClusteringExternal<V, E>> {
        /**
         * The default value of the inflation parameter.
         */
        public static final int R = 2;

        /**
         * The default number of threads.
         */
        public static final int THREADS = 1;

        private Path path;
        private double r = R;
        private int threads = THREADS;

        @Override
        public MarkovClusteringExternal<V, E> apply(Graph<V, E> graph) {
            return new MarkovClusteringExternal<>(graph, path, r, threads);
        }

        /**
         * Set the path to the MCL binary.
         *
         * @param path the path to the MCL binary
         * @return the builder
         */
        public Builder<V, E> setPath(Path path) {
            this.path = requireNonNull(path);
            return this;
        }

        /**
         * Set the inflation parameter.
         *
         * @param r the inflation parameter
         * @return the builder
         */
        public Builder<V, E> setR(double r) {
            this.r = r;
            return this;
        }

        /**
         * Set the number of threads.
         *
         * @param threads the number of threads
         * @return the builder
         */
        public Builder<V, E> setThreads(int threads) {
            this.threads = threads;
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

    private static final System.Logger logger = System.getLogger(MarkovClusteringExternal.class.getSimpleName());

    /**
     * The graph.
     */
    protected final Graph<V, E> graph;

    /**
     * The path to the MCL binary.
     */
    protected final Path path;

    /**
     * The inflation parameter.
     */
    protected final double r;

    /**
     * The number of threads.
     */
    protected final int threads;

    /**
     * The cached clustering result.
     */
    protected Clustering<V> clustering;

    /**
     * Create an instance of the Markov Clustering algorithm wrapper.
     *
     * @param graph   the graph
     * @param path    the path to the MCL binary
     * @param r       the inflation parameter
     * @param threads the number of threads
     */
    public MarkovClusteringExternal(Graph<V, E> graph, Path path, double r, int threads) {
        this.graph = requireUndirected(graph);
        this.path = requireNonNull(path);
        this.r = r;
        this.threads = threads;
    }

    @Override
    public Clustering<V> getClustering() {
        if (isNull(clustering)) {
            clustering = new Implementation<>(graph, path, r, threads).compute();
        }

        return clustering;
    }

    /**
     * Actual implementation of the Markov Clustering wrapper.
     *
     * @param <V> the type of nodes in the graph
     * @param <E> the type of edges in the graph
     */
    public static class Implementation<V, E> {
        /**
         * The graph.
         */
        protected final Graph<V, E> graph;

        /**
         * The path to the MCL binary.
         */
        protected final Path path;

        /**
         * The inflation parameter.
         */
        protected final double r;

        /**
         * The number of threads.
         */
        protected final int threads;

        /**
         * The mapping of nodes to indices.
         */
        protected final VertexToIntegerMapping<V> mapping;

        /**
         * The output file.
         */
        protected File output;

        /**
         * Create an instance of the Markov Clustering algorithm wrapper implementation.
         *
         * @param graph   the graph
         * @param path    the path to the MCL binary
         * @param r       the inflation parameter
         * @param threads the number of threads
         */
        public Implementation(Graph<V, E> graph, Path path, double r, int threads) {
            this.graph = graph;
            this.path = path;
            this.r = r;
            this.threads = threads;
            this.mapping = Graphs.getVertexToIntegerMapping(graph);
        }

        /**
         * Perform clustering with Markov Clustering.
         *
         * @return the clustering
         */
        public Clustering<V> compute() {
            logger.log(Level.INFO, "Preparing for Markov Clustering.");

            try {
                process();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }

            logger.log(Level.INFO, "Markov Clustering finished.");

            try (var stream = Files.lines(output.toPath())) {
                final var clusters = stream.map(line -> Arrays.stream(line.split("\t")).
                        map(id -> mapping.getIndexList().get(Integer.parseInt(id))).
                        collect(Collectors.toSet())).
                        collect(Collectors.toList());

                return new ClusteringImpl<>(clusters);
            } catch (IOException ex) {
                throw new IllegalStateException("Clusters cannot be read.", ex);
            }
        }

        /**
         * Run the Markov Clustering binary and read its output.
         *
         * @throws IOException if an I/O error occurs
         */
        protected void process() throws IOException {
            output = File.createTempFile("mcl", "output");
            output.deleteOnExit();

            final var input = writeInputFile();

            final var builder = new ProcessBuilder(
                    path.toAbsolutePath().toString(),
                    input.toString(),
                    "-I", Double.toString(r),
                    "-te", Integer.toString(threads),
                    "--abc",
                    "-o", output.toString());

            logger.log(Level.INFO, () -> "Command: " + String.join(" ", builder.command()));

            final var process = builder.start();

            int status;

            try {
                status = process.waitFor();
            } catch (InterruptedException e) {
                // TODO: Is it correct to call interrupt() here?
                Thread.currentThread().interrupt();
                throw new IllegalStateException(path.toAbsolutePath() + " has been interrupted", e);
            }

            if (status != 0) {
                try (final var isr = new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8)) {
                    try (final var reader = new BufferedReader(isr)) {
                        final var stderr = reader.lines().collect(Collectors.joining(System.lineSeparator()));

                        if (stderr.isEmpty()) {
                            throw new IllegalStateException(path.toAbsolutePath() + " returned " + status);
                        } else {
                            throw new IllegalStateException(path.toAbsolutePath() + " returned " + status + ": " + stderr);
                        }
                    }
                }
            }
        }

        /**
         * Write the input file for the Markov Clustering binary.
         *
         * @return the written input file for the Markov Clustering binary
         * @throws IOException if an I/O error occurs
         */
        protected File writeInputFile() throws IOException {
            final var input = File.createTempFile("mcl", "input");
            input.deleteOnExit();

            try (final var writer = Files.newBufferedWriter(input.toPath())) {
                for (final var edge : graph.edgeSet()) {
                    final int source = mapping.getVertexMap().get(graph.getEdgeSource(edge));
                    final int target = mapping.getVertexMap().get(graph.getEdgeTarget(edge));
                    final var weight = graph.getEdgeWeight(edge);

                    writer.write(String.format(Locale.ROOT, "%d\t%d\t%f%n", source, target, weight));
                }
            }

            return input;
        }
    }
}
