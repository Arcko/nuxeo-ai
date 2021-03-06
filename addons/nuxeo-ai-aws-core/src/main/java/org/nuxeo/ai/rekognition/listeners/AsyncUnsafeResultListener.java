/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     anechaev
 */
package org.nuxeo.ai.rekognition.listeners;

import static org.nuxeo.ai.enrichment.AbstractEnrichmentProvider.MAX_RESULTS;
import static org.nuxeo.ai.enrichment.async.DetectUnsafeImagesEnrichmentProvider.ENRICHMENT_NAME;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.nuxeo.ai.enrichment.EnrichmentMetadata;
import org.nuxeo.ai.enrichment.async.DetectUnsafeImagesEnrichmentProvider;
import org.nuxeo.ai.pipes.types.BlobTextFromDocument;
import org.nuxeo.ai.rekognition.RekognitionService;
import org.nuxeo.ai.services.AIComponent;
import org.nuxeo.runtime.api.Framework;

import com.amazonaws.services.rekognition.model.GetContentModerationRequest;
import com.amazonaws.services.rekognition.model.GetContentModerationResult;

/**
 * Receives a notification from the system to start detect of unsafe content in a video.
 */
public class AsyncUnsafeResultListener extends BaseAsyncResultListener {

    public static final String SUCCESS_EVENT = "asyncRekognitionUnsafeSuccess";

    public static final String FAILURE_EVENT = "asyncRekognitionUnsafeFailure";

    @Override
    protected String getSuccessEventName() {
        return SUCCESS_EVENT;
    }

    @Override
    protected String getFailureEventName() {
        return FAILURE_EVENT;
    }

    @Override
    protected Collection<EnrichmentMetadata> getEnrichmentMetadata(String jobId, Map<String, Serializable> params) {
        int max = (int) params.getOrDefault(MAX_RESULTS, 10);
        GetContentModerationRequest request = new GetContentModerationRequest()
                .withJobId(jobId)
                .withMaxResults(max);

        RekognitionService rs = Framework.getService(RekognitionService.class);
        GetContentModerationResult result = rs.getClient().getContentModeration(request);
        AIComponent service = Framework.getService(AIComponent.class);
        DetectUnsafeImagesEnrichmentProvider es = (DetectUnsafeImagesEnrichmentProvider) service.getEnrichmentProvider(ENRICHMENT_NAME);
        return es.processResult((BlobTextFromDocument) params.get("doc"), (String) params.get("key"), result);
    }
}
