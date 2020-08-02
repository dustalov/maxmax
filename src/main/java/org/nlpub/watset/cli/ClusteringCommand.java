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

package org.nlpub.watset.cli;

import org.nlpub.watset.graph.Clustering;
import org.nlpub.watset.util.ILEFormat;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A command that performs graph clustering.
 */
abstract class ClusteringCommand extends Command {
    /**
     * Create an instance of command.
     *
     * @param parameters the parameters
     */
    public ClusteringCommand(MainParameters parameters) {
        super(parameters);
    }

    public void run() {
        final var clustering = getClustering();
        clustering.fit();

        try (final var writer = newOutputWriter()) {
            ILEFormat.write(writer, clustering);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public abstract Clustering<String> getClustering();
}
