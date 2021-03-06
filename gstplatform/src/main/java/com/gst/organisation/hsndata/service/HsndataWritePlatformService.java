package com.gst.organisation.hsndata.service;

import com.gst.infrastructure.core.api.JsonCommand;
import com.gst.infrastructure.core.data.CommandProcessingResult;

public interface HsndataWritePlatformService {
	
	
	CommandProcessingResult createHsndata(JsonCommand command);
	
	CommandProcessingResult deleteHsndata(final Long id, final JsonCommand command);
	
    CommandProcessingResult updateHsndata(final Long id, final JsonCommand command);
}