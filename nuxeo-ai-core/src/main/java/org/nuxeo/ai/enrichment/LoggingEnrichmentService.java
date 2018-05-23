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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.stream.pipes.types.BlobTextStream;

/**
 * Just writes to a log
 */
public class LoggingEnrichmentService extends AbstractEnrichmentService {

    private static final Log log = LogFactory.getLog(LoggingEnrichmentService.class);

    @Override
    public EnrichmentMetadata enrich(BlobTextStream blobTextStream) {
        log.info("Logging: " + blobTextStream);
        return null;
    }
}