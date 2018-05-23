/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Gethin James
 */
package org.nuxeo.ai.enrichment;

import org.nuxeo.runtime.stream.pipes.types.BlobTextStream;

import net.jodah.failsafe.RetryPolicy;

/**
 * An enricher that throws errors
 */
public class ErroringEnrichmentService extends AbstractEnrichmentService {

    private RuntimeException exception;
    private int numFailures = 1;
    private int numRetries = 0;
    private int attempts = 0;

    public ErroringEnrichmentService(RuntimeException exception, int numFailures, int numRetries) {
        super();
        this.exception = exception;
        this.numFailures = numFailures;
        this.numRetries = numRetries;
        this.maxSize = EnrichmentDescriptor.DEFAULT_MAX_SIZE;
        this.modelVersion = "EnRich1";
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return super.getRetryPolicy().withMaxRetries(numRetries);
    }

    @Override
    public EnrichmentMetadata enrich(BlobTextStream blobTextStream) {
        if (++attempts <= numFailures) {
            throw exception;
        }
        return new EnrichmentMetadata.Builder("ErroringEnricher", modelVersion, "myDocId").build();
    }
}